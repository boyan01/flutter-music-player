// MusicResult.aidl
package tech.soit.quiet;

import tech.soit.quiet.player.MusicMetadata;

interface MusicResult {
    void onResult(in MusicMetadata metadata);
}
