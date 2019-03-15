package neo.io.actors;

/**
 * Customized idle akka message
 */
public final class Idle {

    /**
     * get an idle message instance
     *
     * @return idle message
     */
    public static Idle instance() {
        return new Idle();
    }

}
