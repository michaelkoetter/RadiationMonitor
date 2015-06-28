package de.mkoetter.radmon;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.BaseSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;


public class NotificationGraphActivity extends Activity {

    private GraphView graphView;
    private BaseSeries<DataPoint> graphViewSeries;

    public static final String EXTRA_DATA_POINTS = "dataPoints";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CONTENT = "content";

    private static final String TAG = "NotificationGraphAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        setContentView(R.layout.notification_graph);

        long[] dataPoints = getIntent().getLongArrayExtra(EXTRA_DATA_POINTS);
        CharSequence title = getIntent().getCharSequenceExtra(EXTRA_TITLE);

        if (title != null) {
            TextView txtTitle = (TextView)findViewById(R.id.title);
            if (txtTitle != null) txtTitle.setText(title);
        }

        if (dataPoints != null) {
            graphViewSeries = new LineGraphSeries<>();

            ((LineGraphSeries) graphViewSeries).setBackgroundColor(getResources().getColor(R.color.GraphBackgroundColor));
            ((LineGraphSeries) graphViewSeries).setDrawBackground(true);

            Paint linePaint = new Paint();
            linePaint.setColor(getResources().getColor(R.color.GraphLineColor));
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeJoin(Paint.Join.ROUND);
            linePaint.setStrokeWidth(2);
            linePaint.setAntiAlias(true);
            //linePaint.setShadowLayer(2, 1, 1, Color.BLACK);

            ((LineGraphSeries) graphViewSeries).setCustomPaint(linePaint);

            graphView = (GraphView)findViewById(R.id.cpm_graph);

            graphView.getGridLabelRenderer().getStyles().padding = 0;

            graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
            graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
            graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);

            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMinX(0);
            graphView.getViewport().setMaxX(dataPoints.length - 1);

            graphView.getViewport().setYAxisBoundsManual(true);

            long x = 0;
            for (long dataPoint : dataPoints) {
                graphViewSeries.appendData(new DataPoint(x++, dataPoint), false, dataPoints.length);
            }

            graphView.getViewport().setMinY(graphViewSeries.getLowestValueY() - 2);
            graphView.getViewport().setMaxY(graphViewSeries.getHighestValueY() + 2);
            graphView.addSeries(graphViewSeries);

            TextView maxCpm = (TextView)findViewById(R.id.max_cpm);
            TextView minCpm = (TextView)findViewById(R.id.min_cpm);
            if (maxCpm != null) maxCpm.setText(getString(R.string.cpm,
                    Double.valueOf(graphViewSeries.getHighestValueY()).longValue()));
            if (minCpm != null) minCpm.setText(getString(R.string.cpm,
                    Double.valueOf(graphViewSeries.getLowestValueY()).longValue()));
        }

    }


}
