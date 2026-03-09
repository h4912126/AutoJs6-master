package org.autojs.autojs.ui.floating.layoutinspector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.autojs.autojs.core.accessibility.AssistNodeInfo;
import org.autojs.autojs.ui.widget.LevelBeamView;
import org.autojs.autojs.util.ViewUtils;
import org.autojs.autojs6.R;

import java.util.*;

import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListAdapter;
import pl.openrnd.multilevellistview.MultiLevelListView;
import pl.openrnd.multilevellistview.NestType;
import pl.openrnd.multilevellistview.OnItemClickListener;

/**
 * Digital Assistant version of LayoutHierarchyView.
 * Displays layout hierarchy using AssistNodeInfo instead of NodeInfo.
 */
public class AssistantLayoutHierarchyView extends MultiLevelListView {

    public interface OnItemLongClickListener {
        void onItemLongClick(View view, AssistNodeInfo nodeInfo);
    }

    private final Map<AssistNodeInfo, ViewHolder> nodeMap = new LinkedHashMap<>();
    private Adapter mAdapter;
    private OnItemLongClickListener mOnItemLongClickListener;
    private final AdapterView.OnItemLongClickListener mOnItemLongClickListenerProxy = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (mOnItemLongClickListener != null) {
                mOnItemLongClickListener.onItemLongClick(view, ((ViewHolder) view.getTag()).nodeInfo);
                return true;
            }
            return false;
        }

    };

    private Paint mPaint;
    private int[] mBoundsInScreen;
    private int mStatusBarHeight;
    private AssistNodeInfo mClickedNodeInfo;
    private View mClickedView;
    private Drawable mOriginalBackground;

    private boolean mShowClickedNodeBounds;
    private int mClickedColor = 0x99b2b3b7;
    private AssistNodeInfo mRootNode;
    private final Set<AssistNodeInfo> mInitiallyExpandedNodes = new HashSet<>();

    public AssistantLayoutHierarchyView(Context context) {
        super(context);
        init();
    }

    public AssistantLayoutHierarchyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AssistantLayoutHierarchyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setShowClickedNodeBounds(boolean showClickedNodeBounds) {
        mShowClickedNodeBounds = showClickedNodeBounds;
    }

    public void setClickedBackgroundColor(int clickedColor) {
        mClickedColor = clickedColor;
    }

    private void init() {
        mAdapter = new Adapter();
        setAdapter(mAdapter);
        setNestType(NestType.MULTIPLE);
        ((ListView) getChildAt(0)).setOnItemLongClickListener(mOnItemLongClickListenerProxy);
        setWillNotDraw(false);
        initPaint();
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
                setClickedItem(view, (AssistNodeInfo) item);
            }

            @Override
            public void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
                setClickedItem(view, (AssistNodeInfo) item);
            }
        });
    }

    private void setClickedItem(View view, AssistNodeInfo item) {
        if (mClickedView == null) {
            mOriginalBackground = view.getBackground();
        } else {
            mClickedView.setBackground(mOriginalBackground);
        }
        view.setBackgroundColor(mClickedColor);
        mClickedNodeInfo = item;
        mClickedView = view;
        invalidate();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(Color.DKGRAY);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(3);
        mStatusBarHeight = ViewUtils.getStatusBarHeightByWindow(getContext());
    }

    public Paint getBoundsPaint() {
        return mPaint;
    }

    public void setRootNode(AssistNodeInfo rootNodeInfo) {
        mRootNode = rootNodeInfo;
        mAdapter.setDataItems(Collections.singletonList(rootNodeInfo));
        mClickedNodeInfo = null;
        mInitiallyExpandedNodes.clear();
    }

    public void setOnItemLongClickListener(final OnItemLongClickListener onNodeInfoSelectListener) {
        mOnItemLongClickListener = onNodeInfoSelectListener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBoundsInScreen == null) {
            mBoundsInScreen = new int[4];
            getLocationOnScreen(mBoundsInScreen);
            mStatusBarHeight = mBoundsInScreen[1];
        }
        if (mShowClickedNodeBounds && mClickedNodeInfo != null) {
            AssistantLayoutBoundsView.drawRect(canvas, mClickedNodeInfo.getBoundsInScreen(), mStatusBarHeight, mPaint);
        }
    }

    public void setSelectedNode(AssistNodeInfo selectedNode) {
        mInitiallyExpandedNodes.clear();
        Stack<AssistNodeInfo> parents = new Stack<>();
        searchNodeParents(selectedNode, mRootNode, parents);
        if (!parents.isEmpty()) {
            mClickedNodeInfo = parents.peek();
            mInitiallyExpandedNodes.addAll(parents);
        }
        mAdapter.reloadData();
    }

    private boolean searchNodeParents(AssistNodeInfo nodeInfo, AssistNodeInfo rootNode, Stack<AssistNodeInfo> stack) {
        stack.push(rootNode);
        if (nodeInfo == rootNode) {
            return true;
        }
        boolean found = false;
        for (AssistNodeInfo child : rootNode.getChildren()) {
            if (searchNodeParents(nodeInfo, child, stack)) {
                found = true;
                break;
            }
        }
        if (!found) {
            stack.pop();
        }
        return found;
    }

    private static class ViewHolder {
        TextView nameView;
        TextView infoView;
        ImageView arrowView;
        LevelBeamView levelBeamView;
        AssistNodeInfo nodeInfo;

        ViewHolder(View view) {
            infoView = view.findViewById(R.id.dataItemInfo);
            nameView = view.findViewById(R.id.dataItemName);
            arrowView = view.findViewById(R.id.dataItemArrow);
            levelBeamView = view.findViewById(R.id.dataItemLevelBeam);
        }
    }

    private class Adapter extends MultiLevelListAdapter {

        @Override
        protected List<?> getSubObjects(Object object) {
            return ((AssistNodeInfo) object).getChildren();
        }

        @Override
        protected boolean isExpandable(Object object) {
            return !((AssistNodeInfo) object).getChildren().isEmpty();
        }

        @Override
        protected boolean isInitiallyExpanded(Object object) {
            return mInitiallyExpandedNodes.contains((AssistNodeInfo) object);
        }

        @SuppressLint("InflateParams")
        @Override
        public View getViewForObject(Object object, View convertView, ItemInfo itemInfo) {
            AssistNodeInfo nodeInfo = (AssistNodeInfo) object;
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_hierarchy_view_item, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            nodeMap.put(nodeInfo, viewHolder);

            viewHolder.nameView.setText(simplifyClassName(nodeInfo.getClassName()));

            viewHolder.nodeInfo = nodeInfo;
            if (viewHolder.infoView.getVisibility() == VISIBLE)
                viewHolder.infoView.setText(getItemInfoDsc(itemInfo));

            if (itemInfo.isExpandable() && !isAlwaysExpanded()) {
                viewHolder.arrowView.setVisibility(View.VISIBLE);
                viewHolder.arrowView.setImageResource(itemInfo.isExpanded()
                        ? R.drawable.arrow_up
                        : R.drawable.arrow_down);
            } else {
                viewHolder.arrowView.setVisibility(View.GONE);
            }

            viewHolder.levelBeamView.setLevel(itemInfo.getLevel());

            if (nodeInfo == mClickedNodeInfo) {
                setClickedItem(convertView, nodeInfo);
            }
            return convertView;
        }

        private String simplifyClassName(CharSequence className) {
            if (className == null) {
                return null;
            }
            String s = className.toString();
            String prefix = "android.widget.";
            if (s.startsWith(prefix)) {
                s = s.substring(prefix.length());
            }
            return s;
        }

        private String getItemInfoDsc(ItemInfo itemInfo) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(Locale.getDefault(), "level[%d], idx in level[%d/%d]",
                    itemInfo.getLevel() + 1,
                    itemInfo.getIdxInLevel() + 1,
                    itemInfo.getLevelSize()));

            if (itemInfo.isExpandable()) {
                builder.append(String.format(", expanded[%b]", itemInfo.isExpanded()));
            }
            return builder.toString();
        }
    }

}
