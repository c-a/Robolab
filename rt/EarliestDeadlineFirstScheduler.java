// Author(s): (Put your name here)
// Notes:     


package rt;

import soccorob.rt.PeriodicParameters;
import soccorob.rt.RLThread;
import soccorob.rt.Scheduler;
import soccorob.rt.time.AbsoluteTime;
import soccorob.rt.time.HighResolutionClock;
import soccorob.rt.time.RelativeTime;
import soccorob.rt.util.EarliestDeadlineSortedList;
import soccorob.rt.util.EarliestReleaseSortedList;
import soccorob.rt.util.RMThreadNode;


/**
 * Earliest First Deadline Scheduler.
 */
public class EarliestDeadlineFirstScheduler extends Scheduler{


	private EarliestDeadlineSortedList readyList;
	private EarliestReleaseSortedList  suspendedList;

	private int finishedTasks = 0;
	private int deadlineMisses = 0;

	public EarliestDeadlineFirstScheduler() { 

		/* Don't wait until JIT optimises execution*/
		super(false);		

		System.out.println("Earliest Deadline First scheduler initiated.");

		RLThread[] threadList = (RLThread[]) getThreads(); 
		RMThreadNode threadNode;

		/* Create a new ready list. Sorted with respect to earliest deadline. */
		readyList = new EarliestDeadlineSortedList();

		/* Create a new suspended list. Sorted with respect to earliest release time. */
		suspendedList = new EarliestReleaseSortedList();	

		/* Move processes to the suspended list */
		for (int i = 0; i < threadList.length; i++) {
			threadNode = new RMThreadNode(threadList[i]);			

			/* set the first release time */
			threadNode.nextRelease.setTime(((PeriodicParameters)threadList[i].getReleaseParameters()).getStart());

			/* set the first deadline */
			threadNode.nextRelease.add(threadNode.thread.getReleaseParameters().getDeadline(),
					threadNode.deadline);

			/* Insert into suspended list */
			suspendedList.insert(threadNode);

		}
		//System.out.println(readyList.toString());
	}

	public String getPolicyName() {
		return "Earliest Deadline First";
	}

	public boolean isFeasible() {
		return true;
	}

	/**
	 * By calling this function the execution stops for the amount of
	 * time specified in timeToSleep.
	 * 
	 * This function should be used somewhere in the scheduler's big
	 * loop, in order to prevent "busy loops" (monopolisation of the CPU
	 * by the scheduler). That is, if there are no tasks ready to run,
	 * then the scheduler should sleep until some taks become so.
	 *
	 * Note that the resolution of this sleep function is in
	 * miliseconds, and that the accuracy of the sleeping time can
	 * vary with a couple of miliseconds.
	 */
	public void sleep(RelativeTime timeToSleep) {

		long microDelta = timeToSleep.toMicroSeconds();

		//resolution is in miliseconds so the 
		//microseconds are divided by 1000
		if((long)(microDelta/1000) > 0) {
			try {
				synchronized (this) {
					this.wait(microDelta/1000);
				}
			}
			catch (InterruptedException ie){
				System.out.println("The wait was interrupted");
				ie.printStackTrace();
			}
		}
	}



	/**
	 * Priority driven scheduling. Your task is to make a scheduler by
	 * filling our the part within the while loop.
	 */
	public void start() {		

		/* Create some objects needed */
		AbsoluteTime absTime = new AbsoluteTime();

		RMThreadNode readyThreadNode;
		RLThread readyThread;

		RMThreadNode suspendedThreadNode;
		RLThread suspendedThread;

		/* Initialize the clock */
		HighResolutionClock.resetClock();

		while (true) {
			
			HighResolutionClock.getTime(absTime);
			// Move processes to the ready list
			while (!suspendedList.isEmpty())
			{
				suspendedThreadNode = (RMThreadNode) suspendedList.getFirst();
				suspendedThread = suspendedThreadNode.thread;
				
				if (absTime.isGreater(suspendedThreadNode.nextRelease))
				{
					suspendedList.removeFirst();

					suspendedThread.setReady();
					readyList.insert(suspendedThreadNode);
				}
				else
					break;
			}
			
			// Run a ready task
			if (!readyList.isEmpty())
			{
				readyThreadNode = (RMThreadNode) readyList.getFirst();
				readyThread = readyThreadNode.thread;
				
				// Check if the task has missed it deadline
				HighResolutionClock.getTime(absTime);
				if (absTime.isGreater(readyThreadNode.deadline))
				{
					deadlineMisses++;
					
					readyThread.getDeadlineMissHandler().handleAsyncEvent();
				}
				
				else
				{
					fireThread(readyThread);
				}
				
				// Move the task to the suspended list if it's finished
				if (readyThread.isFinished())
				{
					readyList.removeFirst();

					RelativeTime period = ((PeriodicParameters)readyThread.getReleaseParameters()).getPeriod();
					RelativeTime deadline = readyThread.getReleaseParameters().getDeadline();

					readyThreadNode.nextRelease.add(period, readyThreadNode.nextRelease);
					readyThreadNode.nextRelease.add(deadline, readyThreadNode.deadline);

					suspendedList.insert(readyThreadNode);
					
					finishedTasks++;
					printDeadlineMissRatio();
				}
			}
			
			// Sleep until next task is ready
			else
			{
				RelativeTime sleepTime = new RelativeTime();
				HighResolutionClock.getTime(absTime);
				
				suspendedThreadNode = (RMThreadNode) suspendedList.getFirst();
				suspendedThreadNode.nextRelease.subtract(absTime, sleepTime);
				sleep(sleepTime);
			}
				
		}
	}
	
	private void printDeadlineMissRatio()
	{
		System.out.println("Deadline miss ratio: " + ((float)deadlineMisses / finishedTasks));
	}
}	
