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

package page.nafuchoco.neojukepro.nico4njp;

import page.nafuchoco.neobot.api.NeoBot;
import page.nafuchoco.neobot.api.module.NeoModule;
import page.nafuchoco.neojukepro.module.NeoJuke;
import page.nafuchoco.neojukepro.source.NicoAudioSourceManager;

public class Nico4NJP extends NeoModule {

    private static Nico4NJP instance;

    public static Nico4NJP getInstance() {
        if (instance == null)
            instance = (Nico4NJP) NeoBot.getModuleManager().getModule("Nico4NJP");
        return instance;
    }

    @Override
    public void onLoad() {
        NeoJuke neoJuke = (NeoJuke) NeoBot.getModuleManager().getModule("NeoJukePro");
        if (neoJuke != null) {
            neoJuke.getCustomSourceRegistry().registerCustomAudioSource(new NicoAudioSourceManager(), this);
        }
    }
}
