package org.s25rttr.sdl;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.s25rttr.sdl.utils.Filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class TextViewActivity extends Activity {
    private String filePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.text_view);

        Button btn = findViewById(R.id.exitButton);
        btn.setOnClickListener(v -> {
            finish();
        });

        filePath = getIntent().getStringExtra("path");

        RecyclerView recyclerView = findViewById(R.id.lineRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(filePath, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        new Thread(()->{
            try {
                List<Long> offsets =  Filesystem.fileGetLineOffsets(raf);
                runOnUiThread(()->{
                    TextViewAdapter adapter = new TextViewAdapter(offsets, raf);
                    recyclerView.setAdapter(adapter);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();


        Button button = findViewById(R.id.upScrollButton);
        button.setOnClickListener(v -> {
            recyclerView.scrollToPosition(0);
        });

        button = findViewById(R.id.downScrollButton);
        button.setOnClickListener(v -> {
            // -1, da es ja item0 gibt
            recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
        });
    }


    private class LineObject {
        public final String content;

        public LineObject(String content) {
            this.content = content;
        }
    }

    private class TextViewAdapter extends RecyclerView.Adapter<TextViewAdapter.ViewHolder> {
        private List<Long> lines;
        private RandomAccessFile raf;
        private LruCache<Integer, String> cache;

        private static final int maxCacheSize = 1000;

        public TextViewAdapter(List<Long> lineOffsets, RandomAccessFile raf) {
            this.lines = lineOffsets;
            this.raf = raf;

            int lineCount = lines.size();
            if(lineCount < maxCacheSize) {
                cache = new LruCache<>(lineCount);
            } else {
                cache = new LruCache<>(maxCacheSize);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_view_line, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            Long lineOffset = lines.get(position);

            // Set line number
            viewHolder.lineNumView.setText(String.valueOf(position + 1));

            String lineContent;
            if((lineContent = cache.get(position)) != null) {
                viewHolder.content.setText(lineContent);
            } else {
                try {
                    synchronized (raf) {
                        lineContent = Filesystem.fileGetLine(raf, lineOffset);
                    }
                    cache.put(position, lineContent);
                    viewHolder.content.setText(lineContent);
                } catch (IOException e) {
                    viewHolder.content.setText("unable to load");
                }
            }
        }

        @Override
        public int getItemCount() {
            return lines.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView lineNumView, content;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                lineNumView = itemView.findViewById(R.id.lineNumView);
                content = itemView.findViewById(R.id.lineTextView);
            }
        }
    }
}
