package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private FrameLayout mFrameLayout;
    private int hold = 0;
    private boolean mLandscape = false;
    private ArticleListActivity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mFrameLayout = (FrameLayout) findViewById(R.id.landscape_frame);
        if (mFrameLayout != null) { mLandscape = true; }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mAppBarLayout = (AppBarLayout) findViewById(R.id.toolbar_container);

        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapse_toolbar);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setProgressViewOffset(false, 2 * ViewCompat.getMinimumHeight(mToolbar) - 100, 2 * ViewCompat.getMinimumHeight(mToolbar) + 200);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // do actual refresh
                if (mRecyclerView.getChildCount() > 0) {
                    mIsRefreshing = false;
                    updateRefreshingUI();
                }
            }
        });

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (appBarLayout.getHeight() + verticalOffset < 2 * ViewCompat.getMinimumHeight(mToolbar)) {
                    mSwipeRefreshLayout.setEnabled(false);
                } else {
                    mSwipeRefreshLayout.setEnabled(true);
                }
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, ResourcesCompat.getDrawable(getResources(), R.drawable.padded_divider, null), DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int max = mAppBarLayout.getHeight();

                int elev = (mLandscape)
                    ? 3
                    : 0;

                hold = mRecyclerView.computeVerticalScrollOffset();

                if (hold <= ViewCompat.getMinimumHeight(mToolbar)) {
                    mAppBarLayout.setElevation(elev);
                } else {
                    mAppBarLayout.setElevation(mAppBarLayout.getTargetElevation());
                }

                if (dy > 0 && hold <= ViewCompat.getMinimumHeight(mToolbar)) {
                    // scrolling up
                    mAppBarLayout.setTranslationY(Math.max(-max, mAppBarLayout.getTranslationY() - dy));
                } else if (hold <= ViewCompat.getMinimumHeight(mToolbar)){
                    // scrolling down
                    mAppBarLayout.setTranslationY(Math.min(0, mAppBarLayout.getTranslationY() - dy));
                }
            }
        });

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) { refresh(); }
    }

    private void refresh() { startService(new Intent(this, UpdaterService.class)); }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver, new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() { mSwipeRefreshLayout.setRefreshing(mIsRefreshing); }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) { return ArticleLoader.newAllArticlesInstance(this); }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor, activity);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { mRecyclerView.setAdapter(null); }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;
        private ArticleListActivity mActivity;

        public Adapter(Cursor cursor, ArticleListActivity activity) {
            mCursor = cursor;
            mActivity = activity;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                    ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, new Pair<View, String>(vh.thumbnailView, vh.thumbnailView.getTransitionName()));
                    startActivity(intent, activityOptions.toBundle());
                }
            });

            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                DateUtils.getRelativeTimeSpanString(
                    mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                ).toString()
                    + " by "
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
            );

            holder.thumbnailView.setImageUrl(
                mCursor.getString(ArticleLoader.Query.THUMB_URL),
                ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            holder.thumbnailView.setContentDescription(holder.titleView.getText());
        }

        @Override
        public int getItemCount() { return mCursor.getCount(); }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}