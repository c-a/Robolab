// Author(s): (Put your name here)
// Notes:     

package rt;

import soccorob.rt.BaseCyclicScheduler;
import soccorob.rt.RLThread;
import soccorob.rt.time.AbsoluteTime;
import soccorob.rt.time.HighResolutionClock;
import soccorob.rt.time.RelativeTime;


public class CyclicScheduler extends BaseCyclicScheduler {

	private RLThread[] threadList;

	public CyclicScheduler() {
		/* Don't wait until JIT optimises execution*/
		super(false);

		System.out.println("Cyclic scheduler initiated");

		/* Load all tasks in a threadList array */
		this.threadList = (RLThread[])getThreads();		
	}

	public String getPolicyName() {
		return "Cyclic";
	}

	public boolean isFeasible() {
		return true; //not implemented
	}

	/**
	 * Static scheduling. All tasks are run in a predefined order.  Your
	 * task is to make a cyclic scheduler by filling out the part within
	 * the while loop.
	 *
	 * Do not forget to set the length of the minor and major cycles
	 * right after the start of the Scheduler.
	 *
	 * Tasks are in the threadList as follows:
	 * threadList[0] - image processing
	 * threadList[1] - planner for robot 1
	 * threadList[2] - planner for robot 2
	 * threadList[3] - planner for robot 3
	 * threadList[4] - reactor for robot 1
	 * threadList[5] - reactor for robot 2
	 * threadList[6] - reactor for robot 3
	 * threadList[7] - actuator
	 */

	public void start() {

		/* Minor and major cycles should be initialised here ... */
		this.setMajorCycle(new RelativeTime(0, 1, 0));
		this.setMinorCycle(new RelativeTime(0, 0, 50000));
		
		/* Next code signals to the infrastructure the start time of this
           Scheduler */
		AbsoluteTime sstrt = new AbsoluteTime();
		HighResolutionClock.getTime(sstrt);
		setSchedulerStart(sstrt);

		while (true) {			

			/* Minor cycle 1 to 6 */
			for (int i = 1; i <= 6; i++)
			{
				// Run image processing
				fireUntilFinished(threadList[0]);
				
				// Run planner step for robot 1
				fireUntilFinished(threadList[1]);
				// Run planner step for robot 2
				fireUntilFinished(threadList[2]);
				// Run planner step for robot 3
				fireUntilFinished(threadList[3]);
				
				// Run reactors for robots
				fireUntilFinished(threadList[4]);
				fireUntilFinished(threadList[5]);
				fireUntilFinished(threadList[6]);
				
				// Run actuator
				fireUntilFinished(threadList[7]);
				
				waitForCycleInterrupt();
			}
			
			/* Minor cycle 7 to 20 */
			for (int i = 7; i <= 20; i++)
			{
				// Run image processing
				fireUntilFinished(threadList[0]);
				
				// Run reactors for robots
				fireUntilFinished(threadList[4]);
				fireUntilFinished(threadList[5]);
				fireUntilFinished(threadList[6]);
				
				// Run actuator
				fireUntilFinished(threadList[7]);
				
				waitForCycleInterrupt();
			}
		}
	}
}

