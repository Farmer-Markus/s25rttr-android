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

import org.s25rttr.sdl.data.Path;
import org.s25rttr.sdl.utils.UiHelper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class LogViewActivity extends Activity
{
    private volatile boolean indexLog = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_viewer);

        Path logPath = getIntent().getParcelableExtra("log_path");
        if(logPath == null) {
            UiHelper.FatalError(this, "Failed to get log path: 'getIntent().getParcelableExtra(\"log_path\");' is null!");
            return;
        }

        // Setup recyclerview
        RecyclerView recyclerView = findViewById(R.id.LineView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        RandomAccessFile fileReader = null;
        try {
            fileReader = new RandomAccessFile(logPath.toString(), "r");
        } catch (FileNotFoundException e) {
            UiHelper.FatalError(this, e.toString());
            return;
        }

        LogAdapter adapter = new LogAdapter(fileReader);
        recyclerView.setAdapter(adapter);


        TextView textView = findViewById(R.id.TopBarTextView);
        textView.setText(logPath.GetDestName());

        Button button = findViewById(R.id.BackButton);
        button.setOnClickListener(v -> {
            finish();
        });

        button = findViewById(R.id.ScrollBottomButton);
        button.setOnClickListener(v -> {
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        });

        button = findViewById(R.id.ScrollTopButton);
        button.setOnClickListener(v -> {
            recyclerView.scrollToPosition(0);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                //super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if(layoutManager == null) return;

                int itemCount = layoutManager.getItemCount();
                int lastVisible = layoutManager.findLastVisibleItemPosition();

                if(lastVisible >= itemCount - 5) {
                    LogAdapter logAdapter = (LogAdapter) recyclerView.getAdapter();
                    if(logAdapter == null) return;

                    int iCount = logAdapter.getItemCount();
                    if(itemCount != iCount)
                        logAdapter.notifyItemRangeInserted(lastVisible,15);
                }
            }
        });

        new Thread(()->{
            List<Long> offsets;

            RandomAccessFile fileAccess;
            try {
                fileAccess = new RandomAccessFile(logPath.toString(), "r");
            } catch (FileNotFoundException e) {
                runOnUiThread(()->UiHelper.FatalError(this, e.toString()));
                return;
            }

            int updated = 0;

            while(indexLog) {
                try {
                    offsets = GetLineOffsets(fileAccess, 25);
                } catch (IOException e) {
                    runOnUiThread(()->UiHelper.FatalError(this, e.toString()));
                    return;
                }

                if(offsets.isEmpty())
                    break;

                List<Long> finalOffsets = offsets;
                runOnUiThread(() -> adapter.addLines(finalOffsets));
                if(updated < 3) {
                    runOnUiThread(()->adapter.notifyItemRangeInserted(adapter.getItemCount(), finalOffsets.size() - 1));
                    updated++;
                }
            }

            // This dirty little shit!!?!! How am I supposed to do this shit without lagging????
            runOnUiThread(()->adapter.notifyDataSetChanged());
            runOnUiThread(()->Toast.makeText(this, "Finished opening file", Toast.LENGTH_SHORT).show());
            indexLog = false;
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop thread
        indexLog = false;
    }

    private List<Long> GetLineOffsets(RandomAccessFile raf, int toCount) throws IOException {
        List<Long> offsets = new ArrayList<>();
        while(raf.readLine() != null && offsets.size() < toCount)
            offsets.add(raf.getFilePointer());

        return offsets;
    }

    private String GetLine(RandomAccessFile raf, long offset) {
        try {
            raf.seek(offset);
            String line = raf.readLine();
            if(line == null)
                return "";
            return line;
        } catch (IOException e) {
            return "";
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        private RandomAccessFile fileReader;
        private List<Long> lineOffsets;
        private LruCache<Integer, String> lineCache;


        public LogAdapter(RandomAccessFile fileReader) {
            this.fileReader = fileReader;

            lineOffsets = new ArrayList<>();
            lineCache = new LruCache<>(500);
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.log_viewer_line, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            holder.numView.setText(String.valueOf(position));

            // I hate all of this, please someone help me & open a pull request
            String line;
            if((line = lineCache.get(position)) == null)
                lineCache.put(position, (line = GetLine(fileReader, lineOffsets.get(position))));

            holder.contentView.setText(line);
        }

        @Override
        public int getItemCount() {
            return lineOffsets.size();
        }

        public void addLines(List<Long> lineOffsets) {
            this.lineOffsets.addAll(lineOffsets);
        }

        public void addLinesUpdate(List<Long> lineOffsets) {
            int prevSize = this.lineOffsets.size();
            this.lineOffsets.addAll(lineOffsets);
            notifyItemRangeInserted(prevSize, lineOffsets.size());
        }

        class LogViewHolder extends RecyclerView.ViewHolder {
            TextView numView;
            TextView contentView;

            public LogViewHolder(@NonNull View itemView) {
                super(itemView);

                numView = itemView.findViewById(R.id.LineNumView);
                contentView = itemView.findViewById(R.id.LineTextView);
            }
        }
    }
}
