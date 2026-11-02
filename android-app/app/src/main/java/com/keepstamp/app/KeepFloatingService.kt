package com.keepstamp.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class KeepFloatingService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingButton: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var isButtonAdded = false
    private var lastFocusedEditNode: AccessibilityNodeInfo? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingButton()
    }

    private fun createFloatingButton() {
        val btn = TextView(this).apply {
            text = "📅+"
            setTextColor(Color.parseColor("#202124"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            val sizePx = dpToPx(52)
            width = sizePx
            height = sizePx
            elevation = dpToPx(6).toFloat()

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FBBC04"))
                setStroke(dpToPx(2), Color.parseColor("#FFFFFF"))
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            dpToPx(52),
            dpToPx(52),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - dpToPx(72)
            y = resources.displayMetrics.heightPixels / 2
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }
                    layoutParams.x = initialX + dx
                    layoutParams.y = initialY + dy
                    if (isButtonAdded) {
                        windowManager?.updateViewLayout(btn, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        insertDateIntoKeep()
                    }
                    true
                }
                else -> false
            }
        }

        floatingButton = btn
        params = layoutParams
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Sistem klavyesi (Samsung Keyboard / Gboard) veya sistem UI odak aldığında Keep'teysek butonu gizleme
        val isKeyboardOrSystem = pkg.contains("inputmethod") ||
                pkg.contains("honeyboard") ||
                pkg.contains("android.systemui") ||
                pkg == packageName

        if (pkg == "com.google.android.keep") {
            showButton()
            val source = event.source
            if (source != null && source.isEditable) {
                lastFocusedEditNode = source
            }
        } else if (!isKeyboardOrSystem && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            hideButton()
        }
    }

    private fun insertDateIntoKeep() {
        val stamp = DateHelper.getCurrentStamp(this)
        val root = rootInActiveWindow

        val targetNode = findFocusedOrEditableNode(root) ?: lastFocusedEditNode

        if (targetNode != null && targetNode.isEditable) {
            // Önce panoya kopyala ve ACTION_PASTE dene (imlecin tam olduğu yere ekler ve geçmişi bozmaz)
            DateHelper.copyToClipboard(this, showToast = false)
            val pasted = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)

            if (!pasted) {
                val currentText = targetNode.text?.toString() ?: ""
                // Hint (yer tutucu) metinle karışmaması için kontrol
                val hintText = targetNode.hintText?.toString() ?: ""
                val actualText = if (currentText == hintText) "" else currentText

                val newText = if (actualText.isEmpty()) {
                    stamp
                } else {
                    "$actualText\n$stamp"
                }

                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        newText
                    )
                }
                targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
            Toast.makeText(this, "Tarih eklendi: ${stamp.trim()}", Toast.LENGTH_SHORT).show()
        } else {
            DateHelper.copyToClipboard(this, showToast = false)
            Toast.makeText(
                this,
                "Önce notun içine dokunun (Tarih panoya kopyalandı)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun findFocusedOrEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null

        val inputFocused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (inputFocused != null && inputFocused.isEditable) {
            return inputFocused
        }

        // Eğer odaklı bulunamadıysa Keep içindeki not gövdesi EditText'ini bul
        return findFirstEditableRecursive(root)
    }

    private fun findFirstEditableRecursive(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val viewId = node.viewIdResourceName ?: ""
        // Google Keep'te başlık yerine ana not metnine öncelik ver
        if (node.isEditable && !viewId.contains("title", ignoreCase = true)) {
            return node
        }
        var fallbackEditable: AccessibilityNodeInfo? = if (node.isEditable) node else null

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableRecursive(child)
            if (found != null) {
                val foundId = found.viewIdResourceName ?: ""
                if (!foundId.contains("title", ignoreCase = true)) {
                    return found
                }
                if (fallbackEditable == null) fallbackEditable = found
            }
        }
        return fallbackEditable
    }

    private fun showButton() {
        if (!isButtonAdded && floatingButton != null && params != null) {
            try {
                windowManager?.addView(floatingButton, params)
                isButtonAdded = true
            } catch (_: Exception) {
            }
        }
        floatingButton?.visibility = View.VISIBLE
    }

    private fun hideButton() {
        floatingButton?.visibility = View.GONE
    }

    override fun onInterrupt() {
        hideButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isButtonAdded && floatingButton != null) {
            try {
                windowManager?.removeView(floatingButton)
            } catch (_: Exception) {
            }
            isButtonAdded = false
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
