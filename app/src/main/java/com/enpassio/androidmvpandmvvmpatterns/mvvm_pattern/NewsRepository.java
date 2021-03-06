package com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern;


import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.util.Log;

import com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern.data.AppExecutors;
import com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern.data.ArticleBoundaryCallback;
import com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern.data.database.NewsDao;
import com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern.data.database.NewsDatabase;
import com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern.data.model.Article;
import com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern.data.network.APIClient;
import com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern.data.network.LocalCache;
import com.enpassio.androidmvpandmvvmpatterns.mvvm_pattern.data.network.NewsApiService;

import java.util.concurrent.Executor;

/**
 * Created by Greta Grigutė on 2018-11-09.
 */
public class NewsRepository {
    private static final String LOG_TAG = "my_tag";
    private static final int DATABASE_PAGE_SIZE = 10;
    private final NewsApiService newsApiService;
    private NewsDao newsDao;
    private DataSource.Factory<Integer, Article> dataSourceFactory;
    private Executor executor;

    // A constructor that gets a handle to the database and initializes the member variables.
    public NewsRepository(final Application application) {
        NewsDatabase db = NewsDatabase.getDatabase(application);
        newsDao = db.newsDao();
        newsApiService = APIClient.getClient().create(NewsApiService.class);
        executor = AppExecutors.getInstance().diskIO();
    }

    public void insert(final Article article) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                newsDao.insert(article);
            }
        });
    }

    // A wrapper for getAllWords(). Room executes all queries on a separate thread. Observed
    // LiveData will notify the observer when the data has changed.
    public LiveData<PagedList<Article>> getAllNews(String searchQuery) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                newsDao.deleteAll();
            }
        });

        Log.d(LOG_TAG, "Getting the repository");
        Log.v("my_tag", "newsApiService is: " + newsApiService);

        LocalCache localCache = new LocalCache(newsDao, executor);
        // Get data source factory from the local database
        dataSourceFactory = newsDao.getAllNews();

        PagedList.Config pagedListConfig =
                (new PagedList.Config.Builder())
                        .setEnablePlaceholders(false)
                        .setInitialLoadSizeHint(20)
                        .setPageSize(20)
                        .setPrefetchDistance(2)
                        .build();

        ArticleBoundaryCallback boundaryCallback = new ArticleBoundaryCallback(searchQuery, newsApiService, localCache);
        LiveData<PagedList<Article>> allNews =
                new LivePagedListBuilder(dataSourceFactory, pagedListConfig)
                        .setBoundaryCallback(boundaryCallback)
                        .build();

        return allNews;
    }
}
