// FullScreenImageActivity.java
package com.example.falconrepresentator;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.chrisbanes.photoview.PhotoView;
import java.io.File;

public class FullScreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "image_path";
    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        PhotoView photoView = findViewById(R.id.photo_view);
        ImageButton btnClose = findViewById(R.id.btnClose);

        String localPath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        // Use a RequestBuilder to configure Glide more precisely
        RequestBuilder<Drawable> requestBuilder;

        if (localPath != null && !localPath.isEmpty() && !localPath.equals("null")) {
            // If local path exists, load from local file. Bypass Glide's cache.
            requestBuilder = Glide.with(this)
                    .load(new File(localPath))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true);
        } else if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            // If no local path but a valid URL exists, load from URL. Cache the data.
            requestBuilder = Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.DATA);
        } else {
            // No valid image source, load a placeholder or error drawable
            requestBuilder = Glide.with(this).load((Drawable) null);
        }

        requestBuilder.placeholder(R.drawable.image_placeholder)
                .error(R.drawable.error_image)
                .into(photoView);


        // Close the activity when the button is clicked
        btnClose.setOnClickListener(v -> finish());
    }
}
