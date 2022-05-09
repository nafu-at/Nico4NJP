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

import org.jetbrains.annotations.NotNull;

public record NicoSessionInfo(@NotNull String videoUrl,
                              @NotNull String recipeId,
                              @NotNull String contentId,
                              @NotNull String videoSrcIds,
                              @NotNull String audioSrcIds,
                              @NotNull String lifetime,
                              @NotNull String httpParameters,
                              @NotNull String token,
                              @NotNull String signature,
                              @NotNull String authType,
                              @NotNull String contentKeyTimeout,
                              @NotNull String serviceUserId,
                              @NotNull String playerId,
                              @NotNull String priority,
                              @NotNull String sessionUrl) {
}
