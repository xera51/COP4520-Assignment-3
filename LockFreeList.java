import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList<T>
{
	private Node<T> head;

	LockFreeList()
	{
		head = new Node<T>(null);
		head.key = Integer.MIN_VALUE;

		Node<T> tail = new Node<T>(null);
		tail.key = Integer.MAX_VALUE;
		head.next = new AtomicMarkableReference<>(tail, false);

		tail.next = new AtomicMarkableReference<>(null, false);
	}

	public boolean add(T item) {
		int key = item.hashCode();
		while(true) {
			Window<T> window = find(head, key);
			Node<T> pred = window.pred, curr = window.curr;
			if (curr.key == key) {
				return false;
			} else {
				Node<T> node = new Node<>(item);
				node.key = item.hashCode();
				node.next = new AtomicMarkableReference<>(curr, false);
				if (pred.next.compareAndSet(curr, node, false, false)) {
					return true;
				}
			}
		}
	}

	public boolean remove(T item) {
		int key = item.hashCode();
		boolean snip;
		while (true) {
			Window<T> window = find(head, key);
			Node<T> pred = window.pred, curr = window.curr;
			if (curr.key != key) {
				return false;
			} else {
				Node<T> succ = curr.next.getReference();
				snip = curr.next.compareAndSet(succ, succ, false, true);
				if (!snip)
					continue;
				pred.next.compareAndSet(curr, succ, false, false);
				return true;
			}
		}
	}

	public boolean contains(T item) {
		boolean marked[] = {false};
		int key = item.hashCode();
		Node<T> curr = head;
		while (curr.key < key) {
			System.out.println(curr.key);
			curr = curr.next.getReference();
			Node<T> succ = curr.next.get(marked);
		}
		return (curr.key == key && !marked[0]);
	}

	private class Node<T>
	{
		public T item;
		public int key;
		public AtomicMarkableReference<Node<T>> next;

		Node(T item)
		{
			this.item = item;
		}
	}

	private class Window<T>
	{
		public Node<T> pred, curr;

		Window(Node<T> pred, Node<T> curr)
		{
			this.pred = pred;
			this.curr = curr;
		}

	}

	public Window<T> find(Node<T> head, int key)
	{
		Node<T> pred = null, curr = null, succ = null;
		boolean marked[] = {false};
		boolean snip;
		boolean retry;
		while (true) {
			retry = false;
			pred = head;
			curr = pred.next.getReference();
			while(true) {
				succ = curr.next.get(marked);
				while (marked[0]) {
					snip = pred.next.compareAndSet(curr, succ, false, false);
					if (!snip) {
						retry = true;
						break;
					}
					curr = succ;
					succ = curr.next.get(marked);
				}
				if (retry) break;
				if (curr.key >= key)
					return new Window<>(pred, curr);
				pred = curr;
				curr = succ;
			}
		}
	}
}

