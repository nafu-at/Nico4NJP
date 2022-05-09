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
import com.sedmelluq.discord.lavaplayer.container.playlists.ExtendedM3uParser;
import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import okhttp3.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import page.nafuchoco.neojukepro.core.utils.MessageUtil;
import page.nafuchoco.neojukepro.core.utils.URLUtils;
import page.nafuchoco.neojukepro.nico4njp.Nico4NJP;

import java.io.IOException;

public class NicoSegmentUrlProvider extends M3uStreamSegmentUrlProvider {

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    private final NicoSessionInfo sessionInfo;

    private String sessionId;
    private String dataBody;
    private String streamSegmentPlaylistUrl;

    public NicoSegmentUrlProvider(NicoSessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    @Override
    protected String getQualityFromM3uDirective(ExtendedM3uParser.Line directiveLine) {
        return directiveLine.directiveArguments.get("VIDEO");
    }

    @Override
    protected String fetchSegmentPlaylistUrl(HttpInterface httpInterface) throws IOException {

        if (streamSegmentPlaylistUrl == null) {
            RequestBody body = RequestBody.create(buildSessionJson(sessionInfo), MediaType.parse("application/json"));
            Nico4NJP.getInstance().getModuleLogger().debug("RequestInfo : " + buildSessionJson(sessionInfo));
            Request request = new Request.Builder()
                    .url(sessionInfo.sessionUrl() + "?_format=json")
                    .post(body)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseRaw = response.body().string();
                Nico4NJP.getInstance().getModuleLogger().debug("SessionInfo : " + responseRaw);

                JsonObject jsonObject = new Gson().fromJson(responseRaw, JsonObject.class);

                HttpUriRequest httpGet = new HttpGet(jsonObject.getAsJsonObject("data").getAsJsonObject("session").get("content_uri").getAsString());
                URLUtils.URLStructure urlStructure = URLUtils.parseUrl(jsonObject.getAsJsonObject("data").getAsJsonObject("session").get("content_uri").getAsString());
                String prefix = urlStructure.getProtocol() + "://" + urlStructure.getHost() + urlStructure.getPath().replaceAll("master.m3u8*", "");
                String stream = loadStreamsInfo(prefix, HttpClientTools.fetchResponseLines(httpInterface, httpGet, "nicovideo Stream List"));

                sessionId = jsonObject.getAsJsonObject("data").getAsJsonObject("session").get("id").getAsString();
                dataBody = jsonObject.getAsJsonObject("data").toString();

                if (stream == null)
                    throw new IllegalStateException("No streams available.");

                streamSegmentPlaylistUrl = stream;
            }

            return streamSegmentPlaylistUrl;
        }

        RequestBody heartbeatBody = RequestBody.create(dataBody, MediaType.parse("application/json"));
        Nico4NJP.getInstance().getModuleLogger().debug("HeartbeatBody : " + dataBody);
        Request heartbeatRequest = new Request.Builder()
                .url(sessionInfo.sessionUrl() + "/" + sessionId + "?_format=json&_method=PUT")
                .post(heartbeatBody)
                .build();
        try (Response heartbeatResponse = okHttpClient.newCall(heartbeatRequest).execute()) {
            String heartbeatResponseRaw = heartbeatResponse.body().string();
            Nico4NJP.getInstance().getModuleLogger().debug("HeartbeatResponse : " + heartbeatResponseRaw);

            JsonObject jsonObject = new Gson().fromJson(heartbeatResponseRaw, JsonObject.class);
            dataBody = jsonObject.getAsJsonObject("data").toString();
        }

        return streamSegmentPlaylistUrl;
    }

    @Override
    protected HttpUriRequest createSegmentGetRequest(String url) {
        return new HttpGet(url);
    }


    private String buildSessionJson(NicoSessionInfo sessionInfo) {
        String baseJson = """
                {
                    "session": {
                        "recipe_id": "{0}",
                        "content_id": "{1}",
                        "content_type": "movie",
                        "content_src_id_sets": [
                            {
                                "content_src_ids": [
                                    {
                                        "src_id_to_mux": {
                                            "video_src_ids": {2},
                                            "audio_src_ids": {3}
                                        }
                                    }
                                ]
                            }
                        ],
                        "timing_constraint": "unlimited",
                        "keep_method": {
                            "heartbeat": {
                                "lifetime": {4}
                            }
                        },
                        "protocol": {
                            "name": "http",
                            "parameters": {
                                "http_parameters": {5}
                            }
                        },
                        "content_uri": "",
                        "session_operation_auth": {
                            "session_operation_auth_by_signature": {
                                "token": {6},
                                "signature": "{7}"
                            }
                        },
                        "content_auth": {
                            "auth_type": "{8}",
                            "content_key_timeout": {9},
                            "service_id": "nicovideo",
                            "service_user_id": "{10}"
                        },
                        "client_info": {
                            "player_id": "{11}"
                        },
                        "priority": {12}
                    }
                }
                """;

        return MessageUtil.format(baseJson,
                sessionInfo.recipeId(),
                sessionInfo.contentId(),
                sessionInfo.videoSrcIds(),
                sessionInfo.audioSrcIds(),
                sessionInfo.lifetime(),
                sessionInfo.httpParameters(),
                sessionInfo.token(),
                sessionInfo.signature(),
                sessionInfo.authType(),
                sessionInfo.contentKeyTimeout(),
                sessionInfo.serviceUserId(),
                sessionInfo.playerId(),
                sessionInfo.priority());
    }

    private String loadStreamsInfo(String prefix, String[] lines) {
        String infoLine = null;

        for (String lineText : lines) {
            ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

            if (line.lineData != null)
                infoLine = prefix + line.lineData;

            Nico4NJP.getInstance().getModuleLogger().debug("line: " + infoLine);
        }

        return infoLine;
    }
}
