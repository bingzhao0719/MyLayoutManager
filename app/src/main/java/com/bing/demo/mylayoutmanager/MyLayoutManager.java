package com.bing.demo.mylayoutmanager;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class MyLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "MyLayoutManager";
    private int mDecorateWidth;
    private int mDecorateHeight;
    private static final int DEFAULT_COUNT = 1;
    private int mColumnCount = DEFAULT_COUNT;

    private static final int BOTTOM_IN = 1;
    private static final int BOTTOM_OUT = 2;
    private static final int TOP_IN = 3;
    private static final int TOP_OUT = 4;
    public MyLayoutManager() {
        setAutoMeasureEnabled(true);
    }

    public MyLayoutManager(int columnCount) {
        this.mColumnCount = columnCount;
        setAutoMeasureEnabled(true);
    }

    public void setColumnCount(int columnCount) {
        this.mColumnCount = columnCount;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                             ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }
        if (getChildCount() == 0 && state.isPreLayout()) {
            return;
        }
        if (getChildCount() == 0) {
            View scrap = recycler.getViewForPosition(0);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            mDecorateWidth = getDecoratedMeasurementHorizontal(scrap) / mColumnCount;
            mDecorateHeight = getDecoratedMeasurementVertical(scrap);
            detachAndScrapView(scrap, recycler);
        }
        detachAndScrapAttachedViews(recycler);

        //初始化时调用 填充childView
        fill(recycler, state);


    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy,
                                  RecyclerView.Recycler recycler,
                                  RecyclerView.State state) {
        //位移0、没有子View 不移动
        if (dy == 0 || getChildCount() == 0) {
            return 0;
        }
        final View topView = getChildAt(0);
        final View bottomView = getChildAt(getChildCount() - 1);
        if (getDecoratedBottom(bottomView) < getHeight() - getPaddingBottom()){
            //不够一屏不滑动
            return 0;
        }
        int inRowCount = 0;
        int delta = 0;
        if (dy > 0) {
            //手指向上滑动
            //手指向上滑动的时候dy可能过大，超过了子view能移动的距离，需要修正一下
            //1.计算子view所能移动的最大距离，这是因为dy可能大于2倍的item view的高度
            //2.取dy（绝对值）和maxDelta的最小值，代表子view实际要移动的距离
            int bottomOffset = getDecoratedBottom(bottomView);
            int bottomPos = getPosition(bottomView);
            int rowOfBottom = bottomPos / mColumnCount;
            int lastRow = (getItemCount() - 1) / mColumnCount;
            int maxDelta = (lastRow - rowOfBottom) * mDecorateHeight +
                    (bottomOffset - getHeight() + getPaddingBottom());
            //剩余行数
            int leftRow = (getItemCount() + mColumnCount - 2 - bottomPos) / mColumnCount;
            //偏移行数,dy-1是防止dy刚好是mDecorateHeight的倍数
            int dyRow = ((dy - 1) / mDecorateHeight) + 1;
            inRowCount = Math.min(leftRow, dyRow);
            delta = -(Math.min(Math.abs(dy), Math.abs(maxDelta)));
        } else {
            //手指向下滑动
            //手指向下滑动的时候dy可能过大，超过了子view能移动的距离，需要修正一下
            //1.计算子view所能移动的最大距离，这是因为dy可能大于2倍的item view的高度
            //2.取dy（绝对值）和maxDelta的最小值，代表子view实际要移动的距离
            int topOffset = getDecoratedTop(topView);
            int topPos = getPosition(topView);
            //剩余行数
            int leftRow = topPos / mColumnCount;
            //偏移行数
            int dyRow = ((-dy + 1) / mDecorateHeight) + 1;
            inRowCount = Math.min(leftRow, dyRow);
            int maxDelta = leftRow * mDecorateHeight - topOffset;
            delta = Math.min(-dy, maxDelta);
        }
        boolean recycleView = false;
        int direction = -1;
        //以下在填充view的过程中用修正过后的delta作边界判断，因为子view移动的距离是delta而不是dy
        if (dy > 0) {
            //手指向上滑动
            int bottom = getDecoratedBottom(bottomView);
            if (bottom + delta < getHeight() - getPaddingBottom()) {
                //底部有新的view进入屏幕
                recycleView = true;
                direction = BOTTOM_IN;
                fillViewIn(recycler, BOTTOM_IN, inRowCount);

            } else if (getDecoratedBottom(topView) + delta < getPaddingTop()) {
                //topView移出屏幕
                recycleView = true;
                direction = TOP_OUT;
            }

        } else {
            //手指向下滑动
            int top = getDecoratedTop(topView);
            if (top + delta > getPaddingTop()) {
                //头部有新的view进入屏幕
                recycleView = true;
                direction = TOP_IN;
                fillViewIn(recycler, TOP_IN, inRowCount);
            } else if (getDecoratedTop(bottomView) + delta > getHeight() - getPaddingBottom()) {
                //bottomView移出屏幕
                recycleView = true;
                direction = BOTTOM_OUT;
            }
        }
        //先移动子view，再填充
        offsetChildrenVertical(delta);
        if (direction != -1) {
            if (direction == TOP_IN || direction == BOTTOM_IN) {
                recycleViewIn(dy, recycler, recycleView);
            } else if (direction == TOP_OUT || direction == BOTTOM_OUT) {
                recycleViewOut(recycler, direction);
            }
        }


        return -delta;
    }

    private void layoutChild(View child, int left, int top, int right, int bottom) {
        layoutChild(child, left, top, right, bottom, -1);
    }

    private void layoutChild(View child, int left, int top, int right, int bottom, int childIndex) {
        addView(child, childIndex);
        measureChildWithMargins(child, 0, 0);
        layoutDecoratedWithMargins(child, left, top, right, bottom);
    }

    protected void recycleViewIn(int dy, RecyclerView.Recycler recycler, boolean recycleView) {
        int topOffset = getPaddingTop();
        //回收越界子View
        if (getChildCount() > 0 && recycleView) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (dy > 0) {
                    //需要回收当前屏幕，上越界的View
                    if (getDecoratedBottom(child) < topOffset) {
                        removeAndRecycleView(child, recycler);
                    }
                } else if (dy < 0) {
                    //回收当前屏幕，下越界的View
                    if (getDecoratedTop(child) > getHeight() - getPaddingBottom()) {
                        removeAndRecycleView(child, recycler);
                    }
                }
            }
        }
    }


    /**
     * 填充满可见屏幕view，不考虑偏移量
     */
    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int topOffset = getPaddingTop();
        int leftOffset = getPaddingLeft();
        //布局子View阶段
        for (int i = 0; i <= getItemCount() - 1; i++) {
            //计算宽度 包括margin
            if (leftOffset + mDecorateWidth > getHorizontalSpace()) {
                //当前行排列不下,新起一行
                leftOffset = getPaddingLeft();
                topOffset += mDecorateHeight;
                if (topOffset > getHeight() - getPaddingBottom()) {
                    break;
                }

            }
            View child = recycler.getViewForPosition(i);
            layoutChild(child, leftOffset, topOffset,
                        leftOffset + mDecorateWidth,
                        topOffset + mDecorateHeight);
            leftOffset += mDecorateWidth;

        }
    }

    /**
     * 填充子view，考虑偏移量
     */
    private void fillViewIn(RecyclerView.Recycler recycler, int direction, int intRowCount) {
        if (getChildCount() == 0) {
            return;
        }
        View topView = getChildAt(0);
        View bottomView = getChildAt(getChildCount() - 1);
        int topOffset = getDecoratedTop(topView);
        int firstVisiPos = 0;
        int leftOffset = getDecoratedLeft(topView);
        int fillViewCount = 0;
        switch (direction) {
            case TOP_IN:
                topOffset -= mDecorateHeight;
                firstVisiPos = getPosition(topView);
                firstVisiPos -= 1;
                fillViewCount = intRowCount * mColumnCount;
                break;
            case BOTTOM_IN:
                firstVisiPos = getPosition(bottomView);
                firstVisiPos += 1;
                topOffset = getDecoratedBottom(bottomView);
                fillViewCount = (intRowCount - 1) * mColumnCount +
                        (getItemCount() - firstVisiPos > mColumnCount ? mColumnCount :
                         getItemCount() - firstVisiPos);
                break;

        }
        firstVisiPos = Math.max(0, firstVisiPos);
        if (direction == BOTTOM_IN) {
            //底部填充，从上到下填充
            for (int i = 0; i < fillViewCount; i++) {
                if ((i + firstVisiPos) > getItemCount() - 1) {
                    break;
                }
                //计算宽度 包括margin
                if (leftOffset + mDecorateWidth > getHorizontalSpace()) {
                    //当前行排列不下,新起一行
                    leftOffset = getPaddingLeft();
                    topOffset += mDecorateHeight;
                }
                View child = recycler.getViewForPosition(i + firstVisiPos);
                layoutChild(child, leftOffset, topOffset,
                            leftOffset + mDecorateWidth,
                            topOffset + mDecorateHeight);
                leftOffset += mDecorateWidth;

            }
        } else if (direction == TOP_IN) {
            //头部填充，从下到上填充，因为要保持子view的position位置
            int rightOffset = getWidth() - getPaddingRight();
            for (int i = 0; i < fillViewCount; firstVisiPos--, i++) {
                if (firstVisiPos < 0) {
                    break;
                }
                //计算宽度 包括margin
                if (rightOffset - mDecorateWidth < getPaddingLeft()) {
                    //当前行排列不下,新起一行
                    rightOffset = getWidth() - getPaddingRight();
                    topOffset -= mDecorateHeight;
                }
                View child = recycler.getViewForPosition(firstVisiPos);
                layoutChild(child, rightOffset - mDecorateWidth, topOffset,
                            rightOffset,
                            topOffset + mDecorateHeight, 0);
                rightOffset -= mDecorateWidth;

            }
        }
    }
    private void recycleViewOut(RecyclerView.Recycler recycler, int direction) {
        if (getChildCount() == 0) {
            return;
        }
        if (direction == TOP_OUT) {
            for (int i = 0; i < mColumnCount; i++) {
                View child = getChildAt(0);
                removeAndRecycleView(child, recycler);
            }
        } else if (direction == BOTTOM_OUT) {
            int lastPos = getPosition(getChildAt(getChildCount() - 1));
            int removeCount = lastPos % mColumnCount + 1;
            for (int i = 0; i < removeCount; i++) {
                View child = getChildAt(getChildCount() - 1);
                removeAndRecycleView(child, recycler);
            }
        }
    }

    /**
     * 获取某个childView在水平方向所占的空间
     *
     * @param view
     *
     * @return
     */
    public int getDecoratedMeasurementHorizontal(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin;
    }

    /**
     * 获取某个childView在竖直方向所占的空间
     *
     * @param view
     *
     * @return
     */
    public int getDecoratedMeasurementVertical(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin;
    }

    public int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }
}

