// ISessionDataProvider.aidl
package tech.soit.quiet;

import tech.soit.quiet.player.MusicMetadata;
import tech.soit.quiet.MusicResult;

parcelable ArtworkData;

interface ISessionDataProvider {

     ArtworkData loadArtwork(in MusicMetadata metadata);

     String getPlayerUrl(String id, String fallbackUrl);

}


