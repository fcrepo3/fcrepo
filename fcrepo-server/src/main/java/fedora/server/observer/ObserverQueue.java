/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.observer;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A threaded FIFO queue to improve performance of updates to Observers.
 * 
 * @author Edwin Shin
 * @version $Id$
 * @see <a
 *      href="http://www.javaworld.com/javaworld/javatips/jw-javatip29.html">http://www.javaworld.com/javaworld/javatips/jw-javatip29.html</a>
 */
public class ObserverQueue
        implements Publisher, Subscriber, Runnable {

    private final Set<Subscriber> subscribers;

    private BlockingQueue<Object> messages;

    public ObserverQueue() {
        subscribers = new CopyOnWriteArraySet<Subscriber>();
        messages = new LinkedBlockingQueue<Object>();
    }

    public void update(Publisher o, Object arg) {
        messages.add(arg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (true) {
            Object obj;
            try {
                obj = messages.take();
                notifySubscribers(obj);
            } catch (InterruptedException e) {
                // restore interrupted status
                Thread.currentThread().interrupt();
            }
        }
    }

    public void addSubscriber(Subscriber obs) {
        subscribers.add(obs);
    }

    public void notifySubscribers() {
        notifySubscribers(null);
    }

    /**
     * Notify subscribers. Notifications are issued using a copy of the
     * subscriber list. Therefore, Subscribers should not assume that a
     * notification will not be received even after calling removeSubscriber.
     * {@inheritDoc}
     */
    public void notifySubscribers(Object o) {
        for (Subscriber subscriber : subscribers) {
            subscriber.update(this, o);
        }
    }

    public void removeSubscriber(Subscriber obs) {
        subscribers.remove(obs);
    }

}
