package com.pyramid.vkuploadimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKPhotoArray;
import com.vk.sdk.api.model.VKWallPostResult;
import com.vk.sdk.api.photo.VKImageParameters;
import com.vk.sdk.api.photo.VKUploadImage;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private String[] scope = new String[]{
            VKScope.MESSAGES,
            VKScope.FRIENDS,
            VKScope.WALL,
            VKScope.PHOTOS,
            VKScope.NOHTTPS,
            VKScope.DOCS
    };

    public static final int TARGET_USER = 178341408;
    public static final int TARGET_ALBUM = 419340559;//TARGET_USER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VKSdk.login(this, scope);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {

                Toast.makeText(getApplicationContext(), "User successfully login",
                        Toast.LENGTH_LONG).show();

                final CheckBox checkBoxWall = (CheckBox) findViewById(R.id.checkBoxWall);
                assert checkBoxWall != null;
                if (checkBoxWall.isChecked()) {

                    checkBoxWall.setChecked(false);
                }

                final CheckBox checkBoxAlbum = (CheckBox) findViewById(R.id.checkBoxAlbum);
                assert checkBoxAlbum != null;
                if (checkBoxAlbum.isChecked()) {

                    checkBoxAlbum.setChecked(false);
                }

                final Button button = (Button) findViewById(R.id.buttonStart);
                assert button != null;
                button.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        if (checkBoxWall.isChecked()) {
                            final Bitmap photo = getPhoto();
                            VKRequest request = VKApi.uploadWallPhotoRequest(new VKUploadImage(photo,
                                    VKImageParameters.jpgImage(0.9f)), 0, TARGET_USER);
                            request.executeWithListener(new VKRequest.VKRequestListener() {

                                @Override
                                public void onComplete(VKResponse response) {
                                    recycleBitmap(photo);
                                    VKApiPhoto photoModel = ((VKPhotoArray) response.parsedModel).get(0);
                                    makePost(new VKAttachments(photoModel));
                                }

                                @Override
                                public void onError(VKError error) {
                                    showError(error);
                                }
                            });
                            Toast.makeText(getApplicationContext(), "Photos had been uploaded to wall",
                                    Toast.LENGTH_LONG).show();
                        }
                        if (checkBoxAlbum.isChecked()) {
                            final Bitmap photo = getPhoto();
                            VKRequest request = VKApi.uploadAlbumPhotoRequest(new VKUploadImage(photo,
                                    VKImageParameters.pngImage()), TARGET_USER, 0);
                            request.executeWithListener(new VKRequest.VKRequestListener() {

                                @Override
                                public void onComplete(VKResponse response) {
                                    recycleBitmap(photo);
                                    VKPhotoArray photoArray = (VKPhotoArray) response.parsedModel;
                                    Intent i = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(String.format("https://vk.com/photo%d_%s", "TARGET_USER", photoArray.get(0).id)));
                                    startActivity(i);
                                }

                                @Override
                                public void onError(VKError error) {
                                    super.onError(error);
                                }
                            });
                            Toast.makeText(getApplicationContext(), "Photos had been uploaded to wall",
                                    Toast.LENGTH_LONG).show();
                        }

                    }
                });

            }

            private void makePost(VKAttachments attachments) {
                makePost(attachments, null);
            }

            private void makePost(VKAttachments attachments, String message) {
                VKRequest post = VKApi.wall().post(VKParameters.from(VKApiConst.OWNER_ID, +
                        TARGET_USER, VKApiConst.ATTACHMENTS, attachments, VKApiConst.MESSAGE, message));
                post.setModelClass(VKWallPostResult.class);
                post.executeWithListener(new VKRequest.VKRequestListener() {

                    @Override
                    public void onComplete(VKResponse response) {
                        VKWallPostResult result = (VKWallPostResult) response.parsedModel;
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://vk.com/wall%d_%s",
                                TARGET_USER, result.post_id)));
                        startActivity(i);
                    }

                    @Override
                    public void onError(VKError error) {
                        showError(error.apiError != null ? error.apiError : error);
                    }
                });

            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(getApplicationContext(), "Произошла ошибка авторизации (например," +
                        " пользователь запретил авторизацию)", Toast.LENGTH_LONG).show();
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private Bitmap getPhoto() {
        try {
            return BitmapFactory.decodeStream(this.getAssets().open("android_vk.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showError(VKError error) {
        new AlertDialog.Builder(this)
                .setMessage(error.toString())
                .setPositiveButton("OK", null)
                .show();
        if (error.httpError != null) {
            Log.w("Test", "Error in request or upload", error.httpError);
        }
    }

    private static void recycleBitmap(@Nullable final Bitmap bitmap) {
        if (bitmap != null) {
            bitmap.recycle();
        }
    }


}
