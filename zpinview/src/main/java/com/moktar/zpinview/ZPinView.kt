package com.moktar.zpinview

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.text.TextPaint
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import kotlin.math.abs

class ZPinView : AppCompatEditText {

    object companion {
        const val DEFAULT_COUNT = 3
        const val DEFAULT_BORDER_COLOR = Color.WHITE
        const val DEFAULT_BG_COLOR = Color.BLACK
        const val DEFAULT_TYPE = 0
        const val DEFAULT_BORDER_THICKNESS = 2
    }

    private var mCodeLength = 0
    private var mCodeItemWidth = 0f
    private var mCodeItemHeight = 0f
    private var mCodeItemSpacing = 0f
    private var mRadius = 0f
    private var mDefaultRadius = 0f

    private lateinit var mPaint: Paint
    private lateinit var mTextPaint: TextPaint

    private val mTextRect: Rect = Rect()
    private val mBoxRect = RectF()
    private val mItemCenterPoint = PointF()

    private var mMask: String? = null
    private var onPinCompletionListener: OnPinCompletionListener? = null

    private var mViewType = 1 // line
    private var mBorderColor = 0
    private var mFilledColor = 0
    private var mBorderThickness = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
        initAttributes(context, attrs, defStyle)
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
        init()
    }

    private fun init() {
        super.setCursorVisible(false)
        super.setTextDirection(View.TEXT_DIRECTION_LTR)
        setTextIsSelectable(false)
        setOnFocusChangeListener { _, _ ->
            invalidate()
        }
    }

    private fun initAttributes(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) {
        val theme: Resources.Theme = context.theme
        val typedArray: TypedArray =
            theme.obtainStyledAttributes(attrs, R.styleable.PinView, defStyleAttr, 0)

        // mCodeLength = use this property to configure code length expected
        mCodeLength = typedArray.getInt(R.styleable.PinView_pinLength, companion.DEFAULT_COUNT)
        // mBorderColor = use this property to configure color of border
        mBorderColor =
            typedArray.getInt(R.styleable.PinView_borderColor, companion.DEFAULT_BORDER_COLOR)
        // mFilledColor = use this property to configure filled background color
        mFilledColor =
            typedArray.getInt(R.styleable.PinView_backgroundColor, companion.DEFAULT_BG_COLOR)
        // mViewType = use this property to configure shape of each char options available are[none,line,rectangle,title]
        mViewType = typedArray.getInt(R.styleable.PinView_viewType, companion.DEFAULT_TYPE)
        // mBorderThickness = use this property to configure thickness of border lines
        mBorderThickness = typedArray.getDimensionPixelSize(
            R.styleable.PinView_borderThickness,
            companion.DEFAULT_BORDER_THICKNESS
        )
        // mMask = use this property to enable mask for each char
        mMask = typedArray.getNonResourceString(R.styleable.PinView_mask)
        typedArray.recycle()

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.style = Paint.Style.STROKE
        mPaint.color = mBorderColor
        mPaint.strokeWidth = mBorderThickness.toFloat()
        mTextPaint = TextPaint()
        mTextPaint.set(paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val width =
            widthSize - paddingStart - paddingEnd

        val unitW: Int = width / mCodeLength

        // update dimensions based on measured width
        mCodeItemSpacing = (unitW * 0.1f)
        mCodeItemWidth = (width / mCodeLength) - mCodeItemSpacing
        mCodeItemHeight = mCodeItemWidth

        mRadius = mCodeItemWidth / 2
        mDefaultRadius = mCodeItemWidth / 4

        setMeasuredDimension(width, unitW + paddingTop + paddingBottom)
    }

    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        if (start != text.length) {
            moveSelectionToEnd()
        }
        if (text.length == mCodeLength) {
            onPinCompletionListener?.onCodeCompletion(text.toString())
        }
    }

    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            moveSelectionToEnd()
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        text?.let {
            if (selEnd != it.length) {
                moveSelectionToEnd()
            }
        }
    }

    private fun moveSelectionToEnd() {
        text?.let {
            setSelection(it.length)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        updatePaints()
        drawCodeUI(canvas)
        canvas.restore()
    }

    private fun updatePaints() {
        mPaint.color = mBorderColor
        paint.color = currentTextColor
    }

    private fun drawCodeUI(canvas: Canvas) {
        var nextItemToFill = -1
        text?.let {
            nextItemToFill = it.length
        }
        for (i in 0 until mCodeLength) {
            val itemSelected = nextItemToFill == i
            val itemFilled = i < nextItemToFill

            prepareCharRect(i, itemSelected)
            updateCenterPoint()
            canvas.save()
            if (itemFilled) {
                drawFilledState(canvas)
                canvas.restore()
            } else {
                if (itemSelected && isFocused) {
                    drawSelectedState(canvas)
                } else {
                    drawDefaultState(canvas)
                }
            }
            text?.let {
                if (it.length > i) {
                    drawCode(canvas, i)
                }
            }
        }
    }

    private fun drawCode(canvas: Canvas, i: Int) {
        if (mMask != null) {
            drawMaskedCode(canvas, i)
        } else {
            drawText(canvas, i)
        }
    }

    private fun drawFilledState(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        when (mViewType) {
            3 -> canvas.drawCircle(mItemCenterPoint.x, mItemCenterPoint.y, mRadius, mPaint)
            2 -> canvas.drawRect(mBoxRect, mPaint)
            1 -> canvas.drawLine(
                mBoxRect.left,
                mBoxRect.bottom,
                mBoxRect.right,
                mBoxRect.bottom,
                mPaint
            )
        }
    }

    private fun drawDefaultCircle(canvas: Canvas) {
        canvas.drawCircle(mItemCenterPoint.x, mItemCenterPoint.y, mDefaultRadius, mPaint)
    }

    private fun drawDefaultLine(canvas: Canvas) {
        val diff = mCodeItemWidth * 0.1f
        canvas.drawLine(
            mBoxRect.left + diff,
            mBoxRect.bottom,
            mBoxRect.right - diff,
            mBoxRect.bottom,
            mPaint
        )
    }


    private fun defaultRect(): RectF {
        val rectDefaultF = RectF()
        val unitW = mCodeItemWidth / 4
        rectDefaultF.left = mBoxRect.left + unitW
        rectDefaultF.right = mBoxRect.right - unitW
        rectDefaultF.top = mBoxRect.top + unitW
        rectDefaultF.bottom = mBoxRect.bottom - unitW
        return rectDefaultF
    }

    private fun drawDefaultRect(canvas: Canvas) {
        canvas.drawRect(defaultRect(), mPaint)
    }

    private fun drawDefaultState(canvas: Canvas) {
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = mBorderThickness * 1f
        when (mViewType) {
            3 -> drawDefaultCircle(canvas)
            2 -> drawDefaultRect(canvas)
            1 -> drawDefaultLine(canvas)
        }
    }

    private fun drawSelectedState(canvas: Canvas) {
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = mBorderThickness * 1.2f
        when (mViewType) {
            3 -> drawSelectedCircle(canvas)
            2 -> drawSelectedRect(canvas)
            1 -> drawSelectedLine(canvas)
        }
    }

    private fun drawSelectedCircle(canvas: Canvas) {
        canvas.drawCircle(mItemCenterPoint.x, mItemCenterPoint.y, mRadius, mPaint)
    }

    private fun drawSelectedLine(canvas: Canvas) {
        canvas.drawLine(mBoxRect.left, mBoxRect.bottom, mBoxRect.right, mBoxRect.bottom, mPaint)
    }


    private fun drawSelectedRect(canvas: Canvas) {
        canvas.drawRect(mBoxRect, mPaint)
    }

    private fun prepareCharRect(i: Int, isSelected: Boolean) {
        val halfViewSpacing = mCodeItemSpacing / 2.0f
        var left = scrollX + paddingLeft + (i * (mCodeItemWidth)) + halfViewSpacing

        var top = scrollY + paddingTop + halfViewSpacing

        if (i > 0) {
            left += mCodeItemSpacing * i
        }
        var right = left + mCodeItemWidth
        var bottom = top + mCodeItemHeight

        if (isSelected && isFocused && mViewType != 0 && mViewType != 1) {
            if (i != 0) {
                left -= halfViewSpacing
            } else {
                right += halfViewSpacing * 2
            }
            if (i != getCode().length - 1) {
                right += halfViewSpacing
            } else {
                left -= halfViewSpacing * 2
            }
            top -= halfViewSpacing
            bottom += halfViewSpacing
        }

        mBoxRect.left = left
        mBoxRect.top = top
        mBoxRect.right = right
        mBoxRect.bottom = bottom
    }

    private fun drawText(canvas: Canvas, i: Int) {
        val paint: Paint = getPaintByIndex(i)
        paint.color = currentTextColor
        drawCodeAtBox(canvas, paint, getCode(), i)
    }

    private fun drawMaskedCode(
        canvas: Canvas,
        i: Int
    ) {
        val paint: Paint = getPaintByIndex(i)
        paint.color = currentTextColor
        mMask?.let {
            drawCodeAtBox(canvas, paint, getCode().replace(".".toRegex(), it), i)
        }
    }

    private fun drawCodeAtBox(
        canvas: Canvas,
        paint: Paint?,
        text: CharSequence,
        charAt: Int
    ) {
        paint?.let {
            it.getTextBounds(text.toString(), charAt, charAt + 1, mTextRect)
            val cx = mItemCenterPoint.x
            val cy = mItemCenterPoint.y
            val x: Float =
                cx - abs(mTextRect.width().toFloat()) / 2 - mTextRect.left
            val y: Float =
                cy + abs(mTextRect.height().toFloat()) / 2 - mTextRect.bottom
            canvas.drawText(text, charAt, charAt + 1, x, y, it)
        }
    }

    private fun getPaintByIndex(i: Int): Paint {
        text?.let {
            if (i == it.length - 1) {
                return mTextPaint
            }
        }
        return paint
    }

    private fun updateCenterPoint() {
        val cx =
            mBoxRect.left + abs(mBoxRect.width()) / 2
        val cy =
            mBoxRect.top + abs(mBoxRect.height()) / 2
        mItemCenterPoint[cx] = cy
    }

    override fun getDefaultMovementMethod(): MovementMethod? {
        return ZPinMovementMethod.instance()
    }

    fun setPinCompletionListener(onPinCompletionListener: OnPinCompletionListener) {
        this.onPinCompletionListener = onPinCompletionListener
    }

    fun getCode(): String {
        text?.let {
            return it.toString().trim()
        }
        return ""
    }

    fun setCode(pin: String) {
        setText(pin)
    }

    fun clear() {
        setText("")
    }
}