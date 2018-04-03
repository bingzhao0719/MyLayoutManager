package com.bing.demo.mylayoutmanager;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Paint mPaint;

    private RecyclerView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        listView = (RecyclerView) findViewById(R.id.listview);
        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                super.onDraw(c, parent, state);
                mPaint.setColor(Color.GREEN);
                int childCount = parent.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View view = parent.getChildAt(i);
                    final int left = view.getLeft();
                    final int top = view.getTop();
                    final int right = view.getRight();
                    final int bottom = view.getBottom();
                    c.drawRect(left - 5, bottom, left + view.getWidth() + 5, bottom + 10, mPaint);
                    //左分隔线
                    c.drawRect(left - 5, top, left, bottom, mPaint);
                    //右分隔线
                    c.drawRect(right, top, right + 5, bottom, mPaint);
                }
            }

            @Override
            public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
                super.onDrawOver(c, parent, state);
            }

            @Override
            public void getItemOffsets(Rect outRect,
                                       View view,
                                       RecyclerView parent,
                                       RecyclerView.State state) {
                outRect.set(5, 0, 5, 10);
            }
        });
        listView.setLayoutManager(new MyLayoutManager(2));
        final ArrayList<Object> objects = initData();
        listView.setAdapter(new RecyclerView.Adapter<MyHolder>() {
            @Override
            public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                TextView textView = new TextView(MainActivity.this);
                textView.setBackgroundResource(R.color.colorAccent);
                textView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 200
                ));
                return new MyHolder(textView);
            }

            @Override
            public void onBindViewHolder(MyHolder holder, final int position) {
                holder.mTextView.setText(String.format(Locale.CHINA,
                                                       "第%02d条数据",
                                                       holder.getLayoutPosition()));
            }

            @Override
            public int getItemCount() {
                return objects.size();
            }
        });
    }
    class MyHolder extends RecyclerView.ViewHolder {

        private TextView mTextView;

        public MyHolder(TextView itemView) {
            super(itemView);
            mTextView = itemView;
        }
    }
    private ArrayList<Object> initData() {
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(null);
        }
        return list;
    }
}
