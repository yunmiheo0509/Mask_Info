package com.example.maskinfo;

import android.location.Location;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.maskinfo.model.Store;
import com.example.maskinfo.model.StoreInfo;
import com.example.maskinfo.repository.MaskService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class MainViewModel extends ViewModel {

    private static final String TAG = MainViewModel.class.getSimpleName();
    //메인에서는 얘가 변경되는 변경점만 감시 원래는 겟터셋터가 필요한데 그냥 퍼블릭으로함.
   public MutableLiveData<List<Store>> itemLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

   public Location location;

    private Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(MaskService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build();

    private MaskService service = retrofit.create(MaskService.class);




    public void fetchStoreInfo(){
        //로딩시작
        loadingLiveData.setValue(true);

        service.fetchStoreInfo(location.getLatitude(),location.getLongitude()).enqueue(new Callback<StoreInfo>() {
            @Override
            public void onResponse(Call<StoreInfo> call, Response<StoreInfo> response) {
                Log.d(TAG,"onResponse: refresh");
                //데이터를 필터링함.
                List<Store> items = response.body().getStores()
                        .stream()
                        .filter(item -> item.getRemainStat() != null)
                        .filter(item -> !item.getRemainStat().equals("empty"))
                        .collect(Collectors.toList());

                for (Store store: items) {
                    double distance = LocationDistance.distance(location.getLatitude(),
                            location.getLongitude(), store.getLat(), store.getLng(), "k");
                    store.setDistance(distance);
                }
                Collections.sort(items);
                itemLiveData.postValue(items);
                //아래에서 자바8의 스트림이용안하면 이렇게 한다.
//                List<Store> result = new ArrayList<>();{
//                    for (int i = 0; i < items.size(); i++) {
//                        Store store = items.get(i);
//                        if (store.getRemainStat() != null) {
//                            result.add(store);
//                        }
//                    }
//                }

//                adapter.updateItems(items.stream().filter(item -> item.getRemainStat() != null)
//                        .collect(Collectors.toList()));
                //로딩끝
                loadingLiveData.setValue(false);

            }

            @Override
            public void onFailure(Call<StoreInfo> call, Throwable t) {
                Log.e(TAG, "onFailure", t);
                itemLiveData.postValue(Collections.emptyList());
                //로딩끝
                loadingLiveData.setValue(false);

            }
        });
    }
}

