package com.example.picturebackend.api.imagesearch;

import com.example.picturebackend.api.imagesearch.model.ImageSearchResult;
import com.example.picturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.example.picturebackend.api.imagesearch.sub.GetImageListApi;
import com.example.picturebackend.api.imagesearch.sub.GetImagePageUrlPageApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlPageApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

    public static void main(String[] args) {
        String url = "https://p3.itc.cn/q_70/images01/20221222/95583d4c535a4a5eba3aabfddd8e3c3f.jpeg";
        List<ImageSearchResult> imageList = searchImage(url);
        System.out.println("搜索成功" + imageList);
    }
}
