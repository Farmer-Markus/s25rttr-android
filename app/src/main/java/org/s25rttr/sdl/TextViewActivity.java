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
import android.widget.Toast;

import org.s25rttr.sdl.utils.Filesystem;
import org.s25rttr.sdl.utils.Ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
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
        // To let user scroll even if text does not fit whole screen
        recyclerView.setMinimumWidth(getResources().getDisplayMetrics().widthPixels - 20);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(filePath, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        TextViewAdapter adapter = new TextViewAdapter(raf);
        recyclerView.setAdapter(adapter);

        new Thread(()->{
            List<Long> offsets;
            long currOffset = 0;

            RandomAccessFile fileAccess;
            try {
                fileAccess = new RandomAccessFile(filePath, "r");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }


            while(true) {
                try {
                    offsets = Filesystem.fileGetLineOffsets(fileAccess, currOffset, 21);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if(offsets.isEmpty())
                    break;

                long lastOffset = offsets.get(offsets.size() - 1);
                if(lastOffset == currOffset) break;

                List<Long> finalOffsets = offsets.subList(0, offsets.size() - 1);
                currOffset = lastOffset;
                runOnUiThread(()->{
                    adapter.addLines(finalOffsets);
                });
            }

            runOnUiThread(()->{
                Toast toast = Toast.makeText(this, "Finished opening file", Toast.LENGTH_SHORT);
                toast.show();
            });
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
        private List<Long> lines = new ArrayList<>(List.of(0L)); // Line offsets
        private final RandomAccessFile raf;
        private LruCache<Integer, String> cache;

        private static final int maxCacheSize = 1024;

        public TextViewAdapter(/*List<Long> lineOffsets, */RandomAccessFile raf) {
            // this.lines = lineOffsets;
            this.raf = raf;

            /*int lineCount = lines.size();
            if(lineCount < maxCacheSize) {
                cache = new LruCache<>(lineCount);
            } else {*/
            cache = new LruCache<>(maxCacheSize);
            //}
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

        public void addLines(List<Long> lineOffsets) {
            int prevSize = this.lines.size();
            this.lines.addAll(lineOffsets);
            notifyItemRangeInserted(prevSize, lineOffsets.size());
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
