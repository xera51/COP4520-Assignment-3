import java.lang.Math;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Presents
{
	private static final int numPresents = 10;
	private static final int numServants = 4;
	private static final double containsChance = 0.0;
	static LockFreeList<Integer> presentsList;
	static Queue<Integer> q;
	static AtomicInteger numThankYousWritten = new AtomicInteger(0);

	public static void main(String [] args) throws InterruptedException
	{
		List<Integer> bag;
		Thread [] servants;

		// Create array of presents and shuffle the order, add to queue
		bag = IntStream.range(0, numPresents).boxed().collect(Collectors.toList());
		//Collections.shuffle(bag);
		q = new ConcurrentLinkedQueue<>();
		q.addAll(bag);

		presentsList = new LockFreeList<>();
		servants = new Thread[numServants];
		
		System.out.println("Running...");
		for (int i = 0; i < numServants; ++i)
		{
			servants[i] = new Thread(new writeThankYou());
			servants[i].start();
		}
		for (int i = 0; i < numServants; ++i)
		{
			servants[i].join();
		}
		System.out.println("Done");
	}

	static class writeThankYou implements Runnable
	{	
		public void run()
		{
			boolean add = true;
			Integer item = null;

			while (!q.isEmpty() || numThankYousWritten.get() < numPresents)
			{
				if (Math.random() < containsChance)
				{
					Integer check = (int) Math.random()*(numPresents);
					presentsList.contains(check);
				}
				else if (add)
				{
					add = false;
					if (!q.isEmpty())
					{
						item = q.poll();
						if (item != null)
						
							while(!presentsList.add(item));
						 
					} else {
						item = null;
					}
				}
				else {
					add = true;
					if (numThankYousWritten.get() < numPresents && item != null)
					{
						while (!presentsList.remove(item));
						numThankYousWritten.getAndIncrement();
					}
				}
			}
		}
	}
}
