package com.example.tacademy.photoprocessing;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.miguelbcr.ui.rx_paparazzo.RxPaparazzo;
import com.miguelbcr.ui.rx_paparazzo.entities.size.ScreenSize;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import java.io.File;

import cn.pedant.SweetAlert.SweetAlertDialog;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    SweetAlertDialog alert;
    ImageView imageView;

    // 파일 업로드
    FirebaseStorage storage = FirebaseStorage.getInstance();
    // 나무 기둥의 주소
    StorageReference storageRef = storage.getReferenceFromUrl("gs://helloworld-8e5c6.appspot.com");

    String havePhoto = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.imageView);
    }

    // 사진이 있는지 없는지 확인(있으면 삭제 혹은 재설정, 없으면 사진선택)
    public void checkNull(View view) {
        if (havePhoto == null) {
            onPhoto(view);
        } else {
            alert =
                    new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("사진삭제")
                            .setContentText("사진을 삭제하실겁니까?")
                            .setConfirmText("YES")
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    deleteImage(havePhoto);
                                }
                            })
                            .setCancelText("Change Photo")
                            .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    alert.dismissWithAnimation();
                                    havePhoto = null;
                                    onPhoto(view);
                                }
                            });
            alert.setCancelable(true);
            alert.show();
        }
    }
    // 사진 선택(카메라, 포토앨범)
    public void onPhoto(View view) {
        alert =
                new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("사진선택")
                        .setContentText("사진을 선택할 방법을 고르세요!!")
                        .setConfirmText("카메라")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                onCamera();
                            }
                        })
                        .setCancelText("포토앨범")
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                onGallery();
                            }
                        });
        alert.setCancelable(true);
        alert.show();
    }
    // 사진찍어 불러오기
    public void onCamera() {

        // 크롭작업을 하기 위해 옵션 설정(편집)
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        options.setMaxBitmapSize(1024 * 1024 * 2);    //1024*1024 = 1MB

        RxPaparazzo.takeImage(this)
                .size(new ScreenSize()) //사이즈(smallSize, ScreenSize, OriginalSize, CustomMaxSize)
                .crop(options)          // 편집
                .useInternalStorage()   //내부 저장 (안쓰면 외부 공용 공간에 앱이름으로 생성됨)
                .usingCamera()          // 카메라 사용
                .subscribeOn(Schedulers.io())   //IO
                .observeOn(AndroidSchedulers.mainThread())  // 스레드 생성
                .subscribe(response -> {    //결과 처리
                    // See response.resultCode() doc
                    // 실패 처리
                    if (response.resultCode() != RESULT_OK) {
                        //     response.targetUI().showUserCanceled();
                        return;
                    }
                    Log.i("PH", response.data());
                    loadImage(response.data());
                    // response.targetUI().loadImage(response.data());
                });
    }
    // 포토앨범에서 불러오기
    public void onGallery() {
      /*  // 크롭작업을 하기 위해 옵션 설정(편집)
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        options.setMaxBitmapSize(1024 * 1024 * 2);    //1024*1024 = 1MB
*/
        RxPaparazzo.takeImage(this)/*
                .size(new ScreenSize())
                .crop(options)
                .useInternalStorage()*/
                .usingGallery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    // See response.resultCode() doc
                    if (response.resultCode() != RESULT_OK) {
                        //  response.targetUI().showUserCanceled();
                        return;
                    }
                    // response.targetUI().loadImage(response.data());
                    Log.i("PH", response.data());
                    loadImage(response.data());
                });
    }
    // 사진 올리기
    public void loadImage(String path) {

        alert.dismissWithAnimation();

        // 이미지뷰에 이미지를 세팅
        // path : /data/user/0/com.example.tacademy.photoprocessing/files/PhotoProcessing/IMG-19012017_044702_481.jpeg
        String url = "file://" + path;

        Picasso.with(this).setLoggingEnabled(true);
        Picasso.with(this).setIndicatorsEnabled(true);
        Picasso.with(this).invalidate(url);
        Picasso.with(this).load(url).into(imageView);

        havePhoto = path;
        uploadImage(path);
    }
    // 파일 업로드
    public void uploadImage(String path) {
        // 내 프로필 사진이 등록되는 최종 경로
        Uri uri = Uri.fromFile(new File(path));
        String uploadName = "profile/" + "**.jpng" + uri.getLastPathSegment();
        // 기둥에 가지 등록
        StorageReference riversRef = storageRef.child(uploadName);
        // 업로드
        UploadTask uploadTask = riversRef.putFile(uri);
        // 이벤트 등록 및 처리

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // 실패 -> 재시도 유도!
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // 성공
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                // downLoadUrl.toString() => 프로필 정보로 업데이트!
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                // 진행률!
                Log.i("KK", taskSnapshot.toString());
            }
        });

    }
    // 업로드된 파일 삭제
    public void deleteImage(String path) {
        havePhoto = null;

        Uri uri = Uri.fromFile(new File(path));
        String uploadName = "profile/" + "**.jpng" + uri.getLastPathSegment();
        StorageReference desertRef = storageRef.child(uploadName);

        desertRef.delete().addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                imageView.setImageDrawable(getResources().getDrawable(R.mipmap.profile));
                havePhoto = null;
                alert.dismissWithAnimation();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        });
    }
}
