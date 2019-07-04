package cmsc433.p3;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.concurrent.*;


/**
 * This file needs to hold your solver to be tested.
 * You can alter the class to extend any class that extends MazeSolver.
 * It must have a constructor that takes in a Maze.
 * It must have a solve() method that returns the datatype List<Direction>
 *   which will either be a reference to a list of steps to take or will
 *   be null if the maze cannot be solved.
 */
public class StudentMTMazeSolver extends SkippingMazeSolver {
    public StudentMTMazeSolver(Maze maze) {
        super(maze);
    }

    public List<Direction> solution = null;

    public List<Direction> solve() {

        ExecutorService pool = Executors.newFixedThreadPool(3);
        Choice init;
        List<Runnable> run = new LinkedList<>();

        try {
            init = firstChoice(maze.getStart());

            while(init.choices.size() > 0) {

                Direction direct = init.choices.peek();
                run.add(new MazeSolverDFS(follow(init.at, direct), init.choices.pop()));
            }

        } catch (SolutionFound solutionFound) {
            solutionFound.printStackTrace();
        }

        for(int i=0; i<run.size(); i++) {

            pool.execute(run.get(i));
        }

        pool.shutdown();

        try{
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }catch (InterruptedException e){}

        return solution;
    }

    /**
     * Depth-first search
     */
    public class MazeSolverDFS implements Runnable {
        Choice newStart;
        Direction newDirection;

        public MazeSolverDFS(Choice newStart, Direction newDirection) {
            this.newStart = newStart;
            this.newDirection = newDirection;
        }

        public void run() {
            LinkedList<Choice> choiceStack = new LinkedList<>();
            Choice ch;

            try {
                choiceStack.push(newStart);
                while (!choiceStack.isEmpty()) {
                    ch = choiceStack.peek();
                    if (ch.isDeadend()) {
                        choiceStack.pop();
                        if (!choiceStack.isEmpty()) choiceStack.peek().choices.pop();
                        continue;
                    }

                    choiceStack.push(follow(ch.at, ch.choices.peek()));
                }
            } catch (SolutionFound e) {
                Iterator<Choice> iter = choiceStack.iterator();
                LinkedList<Direction> solutionPath = new LinkedList<Direction>();


                while (iter.hasNext()) {
                    ch = iter.next();
                    solutionPath.push(ch.choices.peek());
                }
                solutionPath.push(newDirection);

                //System.out.println(solutionPath);
                //if (maze.display != null) maze.display.updateDisplay();
                if (pathToFullPath(solutionPath) != null) {
                    solution = pathToFullPath(solutionPath);

                }
            }

        }

    }
}











