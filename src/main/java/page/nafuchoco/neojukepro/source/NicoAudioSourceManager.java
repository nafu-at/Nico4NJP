/*
 * Copyright 2022 NAFU_at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package page.nafuchoco.neojukepro.source;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import page.nafuchoco.neojukepro.core.MessageManager;
import page.nafuchoco.neojukepro.core.player.CustomAudioSourceManager;
import page.nafuchoco.neojukepro.core.player.NeoGuildPlayer;
import page.nafuchoco.neojukepro.core.utils.MessageUtil;
import page.nafuchoco.neojukepro.nico4njp.Nico4NJP;

import java.awt.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class NicoAudioSourceManager implements CustomAudioSourceManager, HttpConfigurable {
    private static final String NICO_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)nicovideo\\.jp/watch/(sm[0-9]+)(?:\\?.*|)$";
    private static final Pattern nicoUrlPattern = Pattern.compile(NICO_URL_REGEX);

    private final HttpInterfaceManager httpInterfaceManager;

    public NicoAudioSourceManager() {
        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    @Override
    public String getSourceName() {
        return "niconico";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        if (!nicoUrlPattern.matcher(reference.getUri()).find())
            return null;

        try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
            try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(reference.identifier))) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (!HttpClientTools.isSuccessWithContent(statusCode))
                    throw new IOException("Unexpected response code from video info: " + statusCode);

                Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
                Elements apiDataDiv = document.getElementsByAttribute("data-api-data");
                String apiData = apiDataDiv.attr("data-api-data");

                Nico4NJP.getInstance().getModuleLogger().debug("apiData: " + apiData);
                return loadTrackFromApiData(reference.identifier, apiData);
            }
        } catch (IOException e) {
            throw new FriendlyException("Loading niconico track information failed", FriendlyException.Severity.SUSPICIOUS, e);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // Nothing special to encode
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        String apiData = getNicoApiData(trackInfo.identifier);
        return new NicoAudioTrack(trackInfo, this, loadSessionFromApiData(trackInfo.identifier, apiData));
    }

    @Override
    public void shutdown() {
        ExceptionTools.closeWithWarnings(httpInterfaceManager);
    }

    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    @Override
    public Color getSourceColor() {
        return Color.BLACK;
    }

    @Override
    public MessageEmbed getNowPlayingEmbed(NeoGuildPlayer audioPlayer) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(audioPlayer.getPlayingTrack().getTrack().getInfo().title, audioPlayer.getPlayingTrack().getTrack().getInfo().uri);
        builder.setColor(getSourceColor());
        builder.setAuthor(audioPlayer.getPlayingTrack().getTrack().getInfo().author);
        //builder.setThumbnail(streamInfo.getThumbnail().toString());
        MessageEmbed.Field time = new MessageEmbed.Field("Time",
                "[" + MessageUtil.formatTime(audioPlayer.getTrackPosition()) + "/" + MessageUtil.formatTime(audioPlayer.getPlayer().getPlayingTrack().getDuration()) + "]",
                true);
        builder.addField(time);
        //MessageEmbed.Field description = new MessageEmbed.Field("Description", streamInfo.getUserDescription(), false);
        //builder.addField(description);
        //MessageEmbed.Field source = new MessageEmbed.Field("",
        //        "Loaded from " + guildAudioPlayer.getNowPlaying().getTrack().getSourceManager().getSourceName() + ".", false);
        //builder.addField(source);
        builder.setFooter(MessageUtil.format(MessageManager.getMessage("command.nowplay.request"), audioPlayer.getPlayingTrack().getInvoker().getEffectiveName()),
                audioPlayer.getPlayingTrack().getInvoker().getUser().getAvatarUrl());
        return builder.build();
    }


    private String getNicoApiData(String videoUrl) {
        try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
            try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(videoUrl))) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (!HttpClientTools.isSuccessWithContent(statusCode))
                    throw new IOException("Unexpected response code from video info: " + statusCode);

                Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
                Elements apiDataDiv = document.getElementsByAttribute("data-api-data");
                String apiData = apiDataDiv.attr("data-api-data");

                Nico4NJP.getInstance().getModuleLogger().debug("apiData: " + apiData);
                return apiData;
            }
        } catch (IOException e) {
            throw new FriendlyException("Loading niconico track information failed", FriendlyException.Severity.SUSPICIOUS, e);
        }
    }

    private NicoSessionInfo loadSessionFromApiData(String trackUrl, String apiData) {
        JsonObject jsonObject = new Gson().fromJson(apiData, JsonObject.class);
        JsonObject sessionApiData = jsonObject.getAsJsonObject("media").getAsJsonObject("delivery").getAsJsonObject("movie").getAsJsonObject("session");

        String videoUrl = trackUrl;
        String recipeId = sessionApiData.get("recipeId").getAsString();
        String contentId = sessionApiData.get("contentId").getAsString();
        String videoSrcIds = sessionApiData.getAsJsonArray("videos").toString();
        String audioSrcIds = sessionApiData.getAsJsonArray("audios").toString();
        String lifetime = sessionApiData.get("heartbeatLifetime").getAsString();
        String token = sessionApiData.get("token").toString();
        String signature = sessionApiData.get("signature").getAsString();
        String authType = sessionApiData.getAsJsonObject("authTypes").get(sessionApiData.getAsJsonArray("protocols").get(0).getAsString()).getAsString();
        String contentKeyTimeout = sessionApiData.get("contentKeyTimeout").getAsString();
        String serviceUserId = sessionApiData.get("serviceUserId").getAsString();
        String playerId = sessionApiData.get("playerId").getAsString();
        String priority = sessionApiData.get("priority").getAsString();
        String sessionUrl = (sessionApiData.getAsJsonArray("urls").get(0).getAsJsonObject().get("url").getAsString());

        String httpParameters;
        if (jsonObject.getAsJsonObject("media").getAsJsonObject("delivery").get("encryption").isJsonNull()) {
            String httpParametersJsonBase = """
                    {
                        "parameters": {
                            "hls_parameters": {
                                "use_well_known_port": "{0}",
                                "use_ssl": "{1}",
                                "transfer_preset": "",
                                "segment_duration": 6000
                            }
                        }
                    }
                    """;
            httpParameters = MessageUtil.format(httpParametersJsonBase,
                    BooleanUtils.toString(sessionApiData.getAsJsonArray("urls").get(0).getAsJsonObject().get("isSsl").getAsBoolean(), "yes", "no"),
                    BooleanUtils.toString(sessionApiData.getAsJsonArray("urls").get(0).getAsJsonObject().get("isWellKnownPort").getAsBoolean(), "yes", "no"));
        } else {
            String httpParametersJsonBase = """
                    {
                        "parameters": {
                            "http_output_download_parameters": {
                                "use_ssl": "{0}",
                                "use_well_known_port": "{1}",
                                "transfer_preset": ""
                            }
                        }
                    }
                    """;
            httpParameters = MessageUtil.format(httpParametersJsonBase,
                    BooleanUtils.toString(sessionApiData.getAsJsonObject("urls").get("isSsl").getAsBoolean(), "yes", "no"),
                    BooleanUtils.toString(sessionApiData.getAsJsonObject("urls").get("isWellKnownPort").getAsBoolean(), "yes", "no"));
        }

        return new NicoSessionInfo(videoUrl, recipeId, contentId, videoSrcIds, audioSrcIds, lifetime, httpParameters, token, signature, authType, contentKeyTimeout, serviceUserId, playerId, priority, sessionUrl);
    }

    private AudioTrack loadTrackFromApiData(String trackUrl, String apiData) {
        JsonObject jsonObject = new Gson().fromJson(apiData, JsonObject.class);

        String title = jsonObject.getAsJsonObject("video").get("title").getAsString();
        String author = jsonObject.getAsJsonObject("owner").get("nickname").getAsString();
        String identifier = jsonObject.getAsJsonObject("video").get("id").getAsString();


        return new NicoAudioTrack(new AudioTrackInfo(
                title,
                author,
                Units.CONTENT_LENGTH_UNKNOWN,
                identifier,
                false,
                trackUrl
        ), this, loadSessionFromApiData(trackUrl, apiData));
    }
}
