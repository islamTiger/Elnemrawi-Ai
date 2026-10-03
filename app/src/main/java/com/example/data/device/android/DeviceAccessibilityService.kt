package com.example.data.device.android

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.domain.device.*
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

class DeviceAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: DeviceAccessibilityService? = null
            private set

        val isConnected: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event stream monitoring available for active window / package changes
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    fun pressBack(): DeviceActionResult {
        val performed = performGlobalAction(GLOBAL_ACTION_BACK)
        return if (performed) {
            DeviceActionResult.success("pressBack", "Pressed system back button")
        } else {
            DeviceActionResult.failure("pressBack", DeviceErrorCode.ACTION_FAILED, "Failed to perform global back action")
        }
    }

    fun pressHome(): DeviceActionResult {
        val performed = performGlobalAction(GLOBAL_ACTION_HOME)
        return if (performed) {
            DeviceActionResult.success("pressHome", "Pressed system home button")
        } else {
            DeviceActionResult.failure("pressHome", DeviceErrorCode.ACTION_FAILED, "Failed to perform global home action")
        }
    }

    fun inspectScreen(): DeviceActionResult {
        val rootNode = rootInActiveWindow
            ?: return DeviceActionResult.failure(
                "inspectScreen",
                DeviceErrorCode.ACTION_FAILED,
                "No active window found or screen content is inaccessible"
            )

        val elements = mutableListOf<DeviceElement>()
        try {
            val rootElement = mapNodeToDeviceElement(rootNode)
            elements.add(rootElement)
            return DeviceActionResult.success(
                actionName = "inspectScreen",
                message = "Screen parsed successfully. Found root node and children.",
                elements = elements
            )
        } finally {
            rootNode.recycle()
        }
    }

    fun findElements(
        text: String? = null,
        viewId: String? = null,
        contentDescription: String? = null
    ): DeviceActionResult {
        val rootNode = rootInActiveWindow
            ?: return DeviceActionResult.failure(
                "findElement",
                DeviceErrorCode.ACTION_FAILED,
                "No active window found"
            )

        val matchingElements = mutableListOf<DeviceElement>()
        try {
            searchNodes(rootNode, text, viewId, contentDescription, matchingElements)
            return if (matchingElements.isNotEmpty()) {
                DeviceActionResult.success(
                    actionName = "findElement",
                    message = "Found ${matchingElements.size} matching element(s)",
                    elements = matchingElements
                )
            } else {
                DeviceActionResult.failure(
                    actionName = "findElement",
                    errorCode = DeviceErrorCode.ELEMENT_NOT_FOUND,
                    message = "No elements matching criteria: text='$text', viewId='$viewId', contentDesc='$contentDescription'"
                )
            }
        } finally {
            rootNode.recycle()
        }
    }

    fun clickElement(elementId: String? = null, targetText: String? = null): DeviceActionResult {
        val rootNode = rootInActiveWindow
            ?: return DeviceActionResult.failure(
                "tapElement",
                DeviceErrorCode.ACTION_FAILED,
                "No active window found"
            )

        try {
            val nodeToClick = findNodeForAction(rootNode, elementId, targetText)
                ?: return DeviceActionResult.failure(
                    "tapElement",
                    DeviceErrorCode.ELEMENT_NOT_FOUND,
                    "Target element could not be found to click"
                )

            // Try clicking node or closest clickable ancestor
            var current: AccessibilityNodeInfo? = nodeToClick
            var clicked = false
            while (current != null) {
                if (current.isClickable) {
                    clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) break
                }
                current = current.parent
            }

            if (!clicked) {
                // If standard ACTION_CLICK failed, tap its center coordinates using gesture dispatch
                val rect = Rect()
                nodeToClick.getBoundsInScreen(rect)
                if (!rect.isEmpty) {
                    return tapCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
                }
            }

            return if (clicked) {
                DeviceActionResult.success("tapElement", "Clicked element successfully")
            } else {
                DeviceActionResult.failure("tapElement", DeviceErrorCode.ACTION_FAILED, "Failed to perform click on element")
            }
        } finally {
            rootNode.recycle()
        }
    }

    fun typeText(text: String, elementId: String? = null): DeviceActionResult {
        val rootNode = rootInActiveWindow
            ?: return DeviceActionResult.failure(
                "typeText",
                DeviceErrorCode.ACTION_FAILED,
                "No active window found"
            )

        try {
            val targetNode: AccessibilityNodeInfo? = if (!elementId.isNullOrEmpty()) {
                findNodeById(rootNode, elementId)
            } else {
                rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    ?: findFirstEditableNode(rootNode)
            }

            if (targetNode == null) {
                return DeviceActionResult.failure(
                    "typeText",
                    DeviceErrorCode.ELEMENT_NOT_FOUND,
                    "No focused or editable input field found"
                )
            }

            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            return if (success) {
                DeviceActionResult.success("typeText", "Text entered successfully into input field")
            } else {
                DeviceActionResult.failure("typeText", DeviceErrorCode.ACTION_FAILED, "Failed to set text on input field")
            }
        } finally {
            rootNode.recycle()
        }
    }

    fun scroll(direction: ScrollDirection): DeviceActionResult {
        val rootNode = rootInActiveWindow
            ?: return DeviceActionResult.failure(
                "scroll",
                DeviceErrorCode.ACTION_FAILED,
                "No active window found"
            )

        try {
            val scrollableNode = findFirstScrollableNode(rootNode)
                ?: return DeviceActionResult.failure(
                    "scroll",
                    DeviceErrorCode.ELEMENT_NOT_FOUND,
                    "No scrollable container found in active window"
                )

            val action = when (direction) {
                ScrollDirection.FORWARD, ScrollDirection.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                ScrollDirection.BACKWARD, ScrollDirection.UP -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                ScrollDirection.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                ScrollDirection.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }

            val performed = scrollableNode.performAction(action)
            return if (performed) {
                DeviceActionResult.success("scroll", "Scrolled $direction successfully")
            } else {
                DeviceActionResult.failure("scroll", DeviceErrorCode.ACTION_FAILED, "Scroll action was rejected by view")
            }
        } finally {
            rootNode.recycle()
        }
    }

    fun tapCoordinates(x: Float, y: Float): DeviceActionResult {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val callbackDeferred = CompletableDeferred<Boolean>()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                callbackDeferred.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                callbackDeferred.complete(false)
            }
        }, null)

        return if (dispatched) {
            DeviceActionResult.success("tap", "Tap dispatched at ($x, $y)")
        } else {
            DeviceActionResult.failure("tap", DeviceErrorCode.ACTION_FAILED, "System failed to dispatch tap gesture")
        }
    }

    fun longPressCoordinates(x: Float, y: Float, durationMs: Long = 1000L): DeviceActionResult {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gesture, null, null)
        return if (dispatched) {
            DeviceActionResult.success("longPress", "Long press dispatched at ($x, $y) for ${durationMs}ms")
        } else {
            DeviceActionResult.failure("longPress", DeviceErrorCode.ACTION_FAILED, "Failed to dispatch long press gesture")
        }
    }

    fun swipeCoordinates(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300L): DeviceActionResult {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gesture, null, null)
        return if (dispatched) {
            DeviceActionResult.success("swipe", "Swipe dispatched from ($startX, $startY) to ($endX, $endY)")
        } else {
            DeviceActionResult.failure("swipe", DeviceErrorCode.ACTION_FAILED, "Failed to dispatch swipe gesture")
        }
    }

    // Helper functions for node parsing
    private fun mapNodeToDeviceElement(node: AccessibilityNodeInfo): DeviceElement {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val childElements = mutableListOf<DeviceElement>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            childElements.add(mapNodeToDeviceElement(child))
            child.recycle()
        }

        return DeviceElement(
            id = node.viewIdResourceName ?: UUID.randomUUID().toString(),
            viewIdResourceName = node.viewIdResourceName,
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            className = node.className?.toString(),
            packageName = node.packageName?.toString(),
            bounds = RectBounds(rect.left, rect.top, rect.right, rect.bottom),
            isClickable = node.isClickable,
            isEditable = node.isEditable,
            isScrollable = node.isScrollable,
            isEnabled = node.isEnabled,
            isVisibleToUser = node.isVisibleToUser,
            children = childElements
        )
    }

    private fun searchNodes(
        node: AccessibilityNodeInfo,
        text: String?,
        viewId: String?,
        contentDescription: String?,
        results: MutableList<DeviceElement>
    ) {
        var matches = true
        if (!text.isNullOrEmpty()) {
            val nodeText = node.text?.toString() ?: ""
            if (!nodeText.contains(text, ignoreCase = true)) {
                matches = false
            }
        }
        if (!viewId.isNullOrEmpty()) {
            val nodeViewId = node.viewIdResourceName ?: ""
            if (!nodeViewId.contains(viewId, ignoreCase = true)) {
                matches = false
            }
        }
        if (!contentDescription.isNullOrEmpty()) {
            val nodeDesc = node.contentDescription?.toString() ?: ""
            if (!nodeDesc.contains(contentDescription, ignoreCase = true)) {
                matches = false
            }
        }

        if (matches && (text != null || viewId != null || contentDescription != null)) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            results.add(
                DeviceElement(
                    id = node.viewIdResourceName ?: UUID.randomUUID().toString(),
                    viewIdResourceName = node.viewIdResourceName,
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                    className = node.className?.toString(),
                    packageName = node.packageName?.toString(),
                    bounds = RectBounds(rect.left, rect.top, rect.right, rect.bottom),
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable,
                    isEnabled = node.isEnabled,
                    isVisibleToUser = node.isVisibleToUser
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            searchNodes(child, text, viewId, contentDescription, results)
            child.recycle()
        }
    }

    private fun findNodeForAction(
        node: AccessibilityNodeInfo,
        elementId: String?,
        targetText: String?
    ): AccessibilityNodeInfo? {
        if (!elementId.isNullOrEmpty()) {
            val byId = findNodeById(node, elementId)
            if (byId != null) return byId
        }
        if (!targetText.isNullOrEmpty()) {
            val byText = findNodeByText(node, targetText)
            if (byText != null) return byText
        }
        return null
    }

    private fun findNodeById(node: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        if (node.viewIdResourceName?.contains(viewId, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeById(child, viewId)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.contains(text, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) return found
        }
        return null
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun findFirstScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstScrollableNode(child)
            if (found != null) return found
        }
        return null
    }
}
