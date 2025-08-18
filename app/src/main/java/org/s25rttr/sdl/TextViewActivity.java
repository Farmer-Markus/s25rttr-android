package org.s25rttr.sdl;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.s25rttr.sdl.utils.Filesystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextViewActivity extends Activity {
    private String filePath;
    private boolean linesAreLoading = false;
    private List<LineObject> lineCache = new ArrayList<>();

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
        recyclerView.setMinimumWidth(getResources().getDisplayMetrics().widthPixels - 20);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> lineContent;
        try {
            lineContent = Filesystem.getFileContent(filePath, 100, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<LineObject> lines = new ArrayList<>();
        for(int i = 0; i < lineContent.size(); i++) {
            lines.add(new LineObject(i + 1, lineContent.get(i)));
        }

        TextViewAdapter adapter = new TextViewAdapter(lines);
        recyclerView.setAdapter(adapter);


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(linesAreLoading)
                    return;

                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if(manager == null) return;
                int lastVisible = manager.findLastVisibleItemPosition();
                int itemCount = manager.getItemCount();

                if(lastVisible > (itemCount - 25)) {
                    linesAreLoading = true;
                    recyclerView.post(()->{
                        adapter.addItem(getLines(50, itemCount));
                        linesAreLoading = false;
                    });
                }
            }
        });

        Button button = findViewById(R.id.upScrollButton);
        button.setOnClickListener(v -> {
            recyclerView.smoothScrollToPosition(0);
        });

        button = findViewById(R.id.downScrollButton);
        button.setOnClickListener(v -> {
            recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
        });

    }

    private List<LineObject> getLines(int count, int offset) {
        if(lineCache.size() >= count) {
            List<LineObject> lines = new ArrayList<>(lineCache.subList(0, count));

            // subList is just a pointer so this will delete the first <count> items
            lineCache.subList(0, count).clear();

            refillLineCache(count, offset + count);
            return lines;
        } else {
            int needed = count - lineCache.size();
            List<LineObject> newLines;
            lineCache.clear();
            try {
                List<String> lineContent = Filesystem.getFileContent(filePath, needed, offset + count);
                newLines = new ArrayList<>();
                for (int i = 0; i < lineContent.size(); i++) {
                    newLines.add(new LineObject(offset + lineCache.size() + i + 1, lineContent.get(i)));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            refillLineCache(count, offset + count);
            return newLines;
        }
    }

    private void refillLineCache(int count, int offset) {
        new Thread(()->{
            List<LineObject> newLines;
            try {
                List<String> lineContent = Filesystem.getFileContent(filePath, count, offset);
                newLines = new ArrayList<>();
                for (int i = 0; i < lineContent.size(); i++) {
                    newLines.add(new LineObject(offset + i + 1, lineContent.get(i)));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            lineCache.addAll(newLines);
        }).start();
    }


    private class LineObject {
        public final int lineNum;
        public final String content;

        public LineObject(int lineNum, String content) {
            this.lineNum = lineNum;
            this.content = content;
        }
    }

    private class TextViewAdapter extends RecyclerView.Adapter<TextViewAdapter.ViewHolder> {
        private List<LineObject> lines;

        public TextViewAdapter(List<LineObject> lines) {
            this.lines = lines;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_view_line, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            LineObject line = lines.get(position);

            viewHolder.lineNumView.setText(String.valueOf(line.lineNum));
            viewHolder.content.setText(line.content);
        }

        @Override
        public int getItemCount() {
            return lines.size();
        }

        public void addItem(List<LineObject> lines) {
            int lineLength = this.lines.size();
            this.lines.addAll(lines);
            notifyItemRangeInserted(lineLength, lines.size());
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
