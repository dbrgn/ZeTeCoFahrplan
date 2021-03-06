package ch.dbrgn.fahrplan;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import ch.dbrgn.fahrplan.zeteco.BuildConfig;
import ch.dbrgn.fahrplan.zeteco.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import info.metadude.java.library.brockman.models.Stream;

interface OnCloseDetailListener {

    public void closeDetailView();
}

public class EventDetailFragment extends Fragment {

    private final String LOG_TAG = "Detail";

    public static final String FRAGMENT_TAG = "detail";

    public static final int EVENT_DETAIL_FRAGMENT_REQUEST_CODE = 546;

    private String event_id;

    private String title;

    private Locale locale;

    private Typeface boldCondensed;

    private Typeface black;

    private Typeface light;

    private Typeface regular;

    private Typeface bold;

    private Lecture lecture;

    private int day;

    private String subtitle;

    private String spkr;

    private String abstractt;

    private String descr;

    private String links;

    private String room;

    private Boolean sidePane = false;

    private boolean hasArguments = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        MyApp.LogDebug(LOG_TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (sidePane) {
            return inflater.inflate(R.layout.detail_narrow, container, false);
        } else {
            return inflater.inflate(R.layout.detail, container, false);
        }
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        day = args.getInt(BundleKeys.EVENT_DAY, 0);
        event_id = args.getString(BundleKeys.EVENT_ID);
        title = args.getString(BundleKeys.EVENT_TITLE);
        subtitle = args.getString(BundleKeys.EVENT_SUBTITLE);
        spkr = args.getString(BundleKeys.EVENT_SPEAKERS);
        abstractt = args.getString(BundleKeys.EVENT_ABSTRACT);
        descr = args.getString(BundleKeys.EVENT_DESCRIPTION);
        links = args.getString(BundleKeys.EVENT_LINKS);
        room = args.getString(BundleKeys.EVENT_ROOM);
        sidePane = args.getBoolean(BundleKeys.SIDEPANE, false);
        hasArguments = true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentActivity activity = getActivity();
        if (hasArguments) {
            AssetManager assetManager = activity.getAssets();
            boldCondensed = Typeface.createFromAsset(assetManager, "Roboto-BoldCondensed.ttf");
            black = Typeface.createFromAsset(assetManager, "Roboto-Black.ttf");
            light = Typeface.createFromAsset(assetManager, "Roboto-Light.ttf");
            regular = Typeface.createFromAsset(assetManager, "Roboto-Regular.ttf");
            bold = Typeface.createFromAsset(assetManager, "Roboto-Bold.ttf");

            locale = getResources().getConfiguration().locale;

            FahrplanFragment.loadLectureList(activity, day, false);
            lecture = eventid2Lecture(event_id);

            TextView t;
            t = (TextView) view.findViewById(R.id.date);
            if ((lecture != null) && (lecture.dateUTC > 0)) {
                DateFormat df = SimpleDateFormat
                        .getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
                t.setText(df.format(new Date(lecture.dateUTC)) + " - " + room);
            } else {
                t.setText("");
            }

            t = (TextView) view.findViewById(R.id.lectureid);
            if (t != null) {
                t.setText("ID: " + event_id);
            }

            // Title

            t = (TextView) view.findViewById(R.id.title);
            setUpTextView(t, boldCondensed, title);

            // Subtitle

            t = (TextView) view.findViewById(R.id.subtitle);
            if (TextUtils.isEmpty(subtitle)) {
                t.setVisibility(View.GONE);
            } else {
                setUpTextView(t, light, subtitle);
            }

            // Speakers

            t = (TextView) view.findViewById(R.id.speakers);
            if (TextUtils.isEmpty(spkr)) {
                t.setVisibility(View.GONE);
            } else {
                setUpTextView(t, black, spkr);
            }

            // Abstract

            t = (TextView) view.findViewById(R.id.abstractt);
            if (TextUtils.isEmpty(abstractt)) {
                t.setVisibility(View.GONE);
            } else {
                abstractt = StringUtils.getHtmlLinkFromMarkdown(abstractt);
                setUpHtmlTextView(t, bold, abstractt);
            }

            // Description

            t = (TextView) view.findViewById(R.id.description);
            if (TextUtils.isEmpty(descr)) {
                t.setVisibility(View.GONE);
            } else {
                descr = StringUtils.getHtmlLinkFromMarkdown(descr);
                setUpHtmlTextView(t, regular, descr);
            }

            // Links

            TextView l = (TextView) view.findViewById(R.id.linksSection);
            t = (TextView) view.findViewById(R.id.links);
            if (TextUtils.isEmpty(links)) {
                l.setVisibility(View.GONE);
                t.setVisibility(View.GONE);
            } else {
                l.setTypeface(bold);
                MyApp.LogDebug(LOG_TAG, "show links");
                l.setVisibility(View.VISIBLE);
                links = links.replaceAll("\\),", ")<br>");
                links = StringUtils.getHtmlLinkFromMarkdown(links);
                setUpHtmlTextView(t, regular, links);
            }

            // Media streams (optional)

            l = (TextView) view.findViewById(R.id.streamsSection);
            l.setTypeface(bold);
            t = (TextView) view.findViewById(R.id.streams);
            t.setTypeface(regular);

            String joinedStreamLinks = null;
            String thumbnailUrl = null;
            if (MyApp.offers != null && lecture.room != null) {
                List<Stream> streamsForLectureRoom = MediaStreamsHelper.
                        getStreamsForLectureRoom(MyApp.offers, lecture.room);
                joinedStreamLinks = MediaStreamsHelper
                        .getJoinedStreamLinks(streamsForLectureRoom);
                thumbnailUrl = MediaStreamsHelper
                        .getThumbnailUrlForLectureRoom(MyApp.offers, lecture.room);
            }

            ImageView thumbnailView = (ImageView) view.findViewById(R.id.streamThumbnail);
            if (thumbnailUrl == null) {
                thumbnailView.setVisibility(View.GONE);
            } else {
                Picasso.with(getActivity())
                        .load(thumbnailUrl)
                        .resize(213, 120)
                        .centerCrop()
                        .into(thumbnailView);
                thumbnailView.setVisibility(View.VISIBLE);
            }


            if (joinedStreamLinks == null) {
                l.setVisibility(View.GONE);
                t.setVisibility(View.GONE);
            } else {
                t.setText(Html.fromHtml(joinedStreamLinks), TextView.BufferType.SPANNABLE);
                t.setLinkTextColor(getResources().getColor(R.color.text_link_color));
                t.setMovementMethod(new LinkMovementMethod());
                l.setVisibility(View.VISIBLE);
                t.setVisibility(View.VISIBLE);
            }

            // Event online

            final TextView eventOnlineSection = (TextView) view
                    .findViewById(R.id.eventOnlineSection);
            eventOnlineSection.setTypeface(bold);
            final TextView eventOnlineLink = (TextView) view.findViewById(R.id.eventOnline);
            final String eventUrl = FahrplanMisc.getEventUrl(activity, event_id);
            final String eventLink = "<a href=\"" + eventUrl + "\">" + eventUrl + "</a>";
            setUpHtmlTextView(eventOnlineLink, regular, eventLink);

            activity.supportInvalidateOptionsMenu();
        }
        activity.setResult(FragmentActivity.RESULT_CANCELED);
    }

    private void setUpTextView(@NonNull TextView textView,
                               @NonNull Typeface typeface,
                               @NonNull String text) {
        textView.setTypeface(typeface);
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
    }


    private void setUpHtmlTextView(@NonNull TextView textView,
                                   @NonNull Typeface typeface,
                                   @NonNull String text) {
        textView.setTypeface(typeface);
        textView.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
        textView.setLinkTextColor(ContextCompat.getColor(getActivity(), R.color.text_link_color));
        textView.setMovementMethod(new LinkMovementMethod());
        textView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.detailmenu, menu);
        MenuItem item;
        if (Build.VERSION.SDK_INT < 14) {
            item = menu.findItem(R.id.item_add_to_calendar);
            if (item != null) {
                item.setVisible(false);
            }
        }
        if (lecture != null) {
            if (lecture.highlight) {
                item = menu.findItem(R.id.item_fav);
                if (item != null) {
                    item.setVisible(false);
                }
                item = menu.findItem(R.id.item_unfav);
                if (item != null) {
                    item.setVisible(true);
                }
            }
            if (lecture.has_alarm) {
                item = menu.findItem(R.id.item_set_alarm);
                if (item != null) {
                    item.setVisible(false);
                }
                item = menu.findItem(R.id.item_clear_alarm);
                if (item != null) {
                    item.setVisible(true);
                }
            }
        }
        if (sidePane) {
            item = menu.findItem(R.id.item_close);
            if (item != null) {
                item.setVisible(true);
            }
        }
        item = menu.findItem(R.id.item_nav);
        if (item != null) {
            boolean isVisible = getRoomConvertedForC3Nav() != null;
            item.setVisible(isVisible);
        }
    }

    @Nullable
    private String getRoomConvertedForC3Nav() {
        final String currentRoom = getActivity().getIntent().getStringExtra(BundleKeys.EVENT_ROOM);
        return RoomForC3NavConverter.INSTANCE.convert(BuildConfig.VENUE, currentRoom);
    }

    private Lecture eventid2Lecture(String event_id) {
        if (MyApp.lectureList == null) {
            return null;
        }
        for (Lecture lecture : MyApp.lectureList) {
            if (lecture.lecture_id.equals(event_id)) {
                return lecture;
            }
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EVENT_DETAIL_FRAGMENT_REQUEST_CODE &&
                resultCode == AlarmTimePickerFragment.ALERT_TIME_PICKED_RESULT_CODE) {
            int alarmTimesIndex = data.getIntExtra(
                    AlarmTimePickerFragment.ALARM_PICKED_INTENT_KEY, 0);
            onAlarmTimesIndexPicked(alarmTimesIndex);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showAlarmTimePicker() {
        DialogFragment dialogFragment = new AlarmTimePickerFragment();
        dialogFragment.setTargetFragment(this, EVENT_DETAIL_FRAGMENT_REQUEST_CODE);
        dialogFragment.show(getActivity().getSupportFragmentManager(),
                AlarmTimePickerFragment.FRAGMENT_TAG);
    }

    private void onAlarmTimesIndexPicked(int alarmTimesIndex) {
        if (lecture != null) {
            FahrplanMisc.addAlarm(getActivity(), lecture, alarmTimesIndex);
        }
        getActivity().supportInvalidateOptionsMenu();
        getActivity().setResult(FragmentActivity.RESULT_OK);
        refreshEventMarkers();
    }

    public void refreshEventMarkers() {
        FragmentActivity activity = getActivity();
        if ((activity != null) && (activity instanceof OnRefreshEventMarkers)) {
            ((OnRefreshEventMarkers) activity).refreshEventMarkers();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Lecture l;
        switch (item.getItemId()) {
            case R.id.item_share:
                l = eventid2Lecture(event_id);
                if (l != null) {
                    String formattedLecture = SimpleLectureFormat.format(l);
                    Context context = getContext();
                    if (!LectureSharer.shareSimple(context, formattedLecture)) {
                        Toast.makeText(context, R.string.share_error_activity_not_found, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            case R.id.item_add_to_calendar:
                l = eventid2Lecture(event_id);
                if (l != null) {
                    FahrplanMisc.addToCalender(getActivity(), l);
                }
                return true;
            case R.id.item_fav:
                if (lecture != null) {
                    lecture.highlight = true;
                    FahrplanMisc.writeHighlight(getActivity(), lecture);
                }
                getActivity().supportInvalidateOptionsMenu();
                getActivity().setResult(FragmentActivity.RESULT_OK);
                refreshEventMarkers();
                return true;
            case R.id.item_unfav:
                if (lecture != null) {
                    lecture.highlight = false;
                    FahrplanMisc.writeHighlight(getActivity(), lecture);
                }
                getActivity().supportInvalidateOptionsMenu();
                getActivity().setResult(FragmentActivity.RESULT_OK);
                refreshEventMarkers();
                return true;
            case R.id.item_set_alarm:
                showAlarmTimePicker();
                return true;
            case R.id.item_clear_alarm:
                if (lecture != null) {
                    FahrplanMisc.deleteAlarm(getActivity(), lecture);
                }
                getActivity().supportInvalidateOptionsMenu();
                getActivity().setResult(FragmentActivity.RESULT_OK);
                refreshEventMarkers();
                return true;
            case R.id.item_close:
                FragmentActivity activity = getActivity();
                if ((activity != null) && (activity instanceof OnCloseDetailListener)) {
                    ((OnCloseDetailListener) activity).closeDetailView();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyApp.LogDebug(LOG_TAG, "onDestroy");
    }
}
