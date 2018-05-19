package com.boomapps.steemapp.ui.feeds

import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.boomapps.steemapp.R
import com.boomapps.steemapp.repository.FeedType
import com.boomapps.steemapp.repository.NetworkState
import com.boomapps.steemapp.repository.db.entities.StoryEntity

class FeedListHolder(val type: FeedType, val tabName: String, val feedView: View, val feedViewModel: FeedsViewModel, val callback: FeedListHolderCallback) {
    var refreshLayout: SwipeRefreshLayout = feedView.findViewById(R.id.refreshLayout)
    var recyclerView: RecyclerView = feedView.findViewById(R.id.recyclerView)
    lateinit var adapter: StoriesListAdapter

    init {
        val manager = LinearLayoutManager(feedView.context)
        manager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = manager
        initAdapter()
        initSwipeToRefresh()

    }

    private fun initAdapter(){
        adapter = StoriesListAdapter{
            feedViewModel.retry(type)
        }
        // TODO use real link to Fragment instead callback as Fragment
        recyclerView.adapter = adapter
        feedViewModel.getListLiveData(type).observe(callback as Fragment, Observer<PagedList<StoryEntity>> {
            adapter.submitList(it)
        })
        feedViewModel.getNetworkState(type).observe(callback as Fragment, Observer {
            adapter.setNetworkState(it)
        })
    }

    private fun initSwipeToRefresh() {
        feedViewModel.getRefreshState(type).observe(callback as Fragment, Observer {
            refreshLayout.isRefreshing = it == NetworkState.LOADING
        })
        refreshLayout.setOnRefreshListener {
            feedViewModel.refresh(type)
        }
    }

    fun updateList(newData: PagedList<StoryEntity>) {
        refreshLayout.isRefreshing = false
        adapter.submitList(newData)
    }

    fun setProgressState(enabled: Boolean) {
        refreshLayout.isRefreshing = enabled
    }

}