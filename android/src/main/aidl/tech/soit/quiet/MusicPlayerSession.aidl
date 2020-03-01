// MusicPlayerSession.aidl
package tech.soit.quiet;

import tech.soit.quiet.MusicSessionCallback;
import tech.soit.quiet.MusicResult;
import tech.soit.quiet.player.PlaybackState;
import tech.soit.quiet.player.MusicMetadata;
import tech.soit.quiet.player.PlayQueue;

interface MusicPlayerSession {


    void addCallback(in MusicSessionCallback callback);

    void removeCallback(in MusicSessionCallback callback);

    void destroy();

    // Transport Controls

    /**
     * Request that the player start its playback at its current position.
     */
    void play();


    void playFromMediaId(String mediaId);


    void pause();


    void stop();

    /**
     * Moves to a new location in the media stream.
     *
     * @param pos Position to move to, in milliseconds.
     */
    void seekTo(long pos);


    void skipToNext();

    void skipToPrevious();

    void setPlayMode(int playMode);

    void addMetadata(in MusicMetadata metadata, in String anchorMediaId);

    void removeMetadata(in String mediaId);

    /**
     * Update current play queue
     */
    void setPlayQueue(in PlayQueue queue);

    void getNext(in MusicMetadata anchor, in MusicResult result);

    void getPrevious(in MusicMetadata anchor, in MusicResult result);

    MusicMetadata getCurrent();

    PlayQueue getPlayQueue();

    PlaybackState getPlaybackState();

    int getPlayMode();

}
