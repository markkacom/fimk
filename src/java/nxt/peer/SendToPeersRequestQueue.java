package nxt.peer;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

import nxt.Constants;
import nxt.util.Logger;
import nxt.util.ReadWriteUpdateLock;

import org.json.simple.JSONObject;

public class SendToPeersRequestQueue {
  
    private static class PrioritizedRequest {
        static final AtomicLong seq = new AtomicLong(0);
        long seqNum;
        long pritority;
        JSONObject request;
    
        public PrioritizedRequest(JSONObject request, long priority) {
            this.request = request;
            this.pritority = priority;
            this.seqNum = seq.getAndIncrement();
        }
    }

    private ReadWriteUpdateLock lock = new ReadWriteUpdateLock();
    
    @SuppressWarnings("serial")
    private final PriorityQueue<PrioritizedRequest> waitingRequests = new PriorityQueue<PrioritizedRequest>(
        (PrioritizedRequest p1, PrioritizedRequest p2) -> {
            int result;
            if ((result = Long.compare(p2.pritority, p1.pritority)) == 0) {
                result = Long.compare(p2.seqNum, p1.seqNum);
            }
            return result;
        }
    )
    {
        @Override
        public boolean add(PrioritizedRequest waitingRequest) {
            if (!super.add(waitingRequest)) {
                return false;
            }
            if (size() > Constants.MAX_GOSSIP_QUEUE_LENGTH) {
                PrioritizedRequest removed = remove();
                Logger.logDebugMessage("Dropped waiting request " + removed.request.toJSONString());
            }
            return true;
        }
    };     
    
    public void add(JSONObject request, long priority) {
        lock.writeLock().lock();
        try {
            waitingRequests.add(new PrioritizedRequest(request, priority));
        }
        finally {
            lock.writeLock().unlock();
        }
    }
    
    public JSONObject getNext() {
        lock.writeLock().lock();
        try {
            Iterator<PrioritizedRequest> iterator = waitingRequests.iterator();
            if (iterator.hasNext()) {
                JSONObject request = iterator.next().request;
                iterator.remove();
                return request;
            }
            return null;
        }
        finally {
            lock.writeLock().unlock();
        }
    }
}
