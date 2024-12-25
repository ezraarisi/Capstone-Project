package com.capstonegroupproject.speechtotext;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface UnsplashApiService {
    @Headers("Authorization: Client-ID kV1BRn99GC4gMORX7FwRl-cesUYX0puY2mJDh_4lfCI")
    @GET("search/photos")
    Call<UnsplashSearchResponse> searchPhotos(
        @Query("query") String query,
        @Query("per_page") int perPage
    );
} 