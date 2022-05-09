package page.nafuchoco.neojukepro.source;

import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.source.stream.MpegTsM3uStreamAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class NicoAudioTrack extends MpegTsM3uStreamAudioTrack {


    private final NicoAudioSourceManager nicoAudioSourceManager;
    private final M3uStreamSegmentUrlProvider segmentUrlProvider;

    /**
     * @param trackInfo              Track info
     * @param nicoAudioSourceManager
     * @param segmentUrlProvider
     */
    public NicoAudioTrack(AudioTrackInfo trackInfo, NicoAudioSourceManager nicoAudioSourceManager, M3uStreamSegmentUrlProvider segmentUrlProvider) {
        super(trackInfo);
        this.nicoAudioSourceManager = nicoAudioSourceManager;
        this.segmentUrlProvider = segmentUrlProvider;
    }

    @Override
    protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
        return null;
    }

    @Override
    protected HttpInterface getHttpInterface() {
        return null;
    }
}
