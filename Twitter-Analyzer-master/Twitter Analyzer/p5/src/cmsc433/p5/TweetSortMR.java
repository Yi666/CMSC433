package cmsc433.p5;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * Map reduce which sorts the output of {@link TweetPopularityMR}.
 * The input will either be in the form of: </br>
 * 
 * <code></br>
 * &nbsp;(screen_name,  score)</br>
 * &nbsp;(hashtag, score)</br>
 * &nbsp;(tweet_id, score)</br></br>
 * </code>
 * 
 * The output will be in the same form, but with results sorted on the score.
 * 
 */
public class TweetSortMR {

	/**
	 * Minimum <code>int</code> value for a pair to be included in the output.
	 * Pairs with an <code>int</code> less than this value are omitted.
	 */
	private static int CUTOFF = 10;
	
	public static class IntComparator extends WritableComparator{
		public IntComparator() {
			super(IntWritable.class);
		}
		@Override
		public int compare(byte[]b1,int s1,int l1, byte[] b2,int s2,int l2) {
			Integer v1=ByteBuffer.wrap(b1,s1,l1).getInt();
			Integer v2=ByteBuffer.wrap(b2,s2,l2).getInt();
			return v1.compareTo(v2)*(-1);
		}
		
	}

	public static class SwapMapper
	extends Mapper<Object, Text,IntWritable,Text /* TODO: fill in the rest of the generic type arguments */> {

		String      id;
		int         score;

		@Override
		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			String[] columns = value.toString().split("\t");
			id = columns[0];
			score = Integer.valueOf(columns[1]);

			// TODO: Your code goes here
			if(CUTOFF<=score) {
				IntWritable sc=new IntWritable(score);
				Text i=new Text(id);
				context.write(sc, i);
			}
		//	System.out.println(id+","+score);



		}
	}

	public static class SwapReducer
	extends Reducer<IntWritable,Text,Text,IntWritable> {

		@Override
		public void reduce(/* fill in type */IntWritable key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			//System.out.println("starting key:"+key.toString());;
			// TODO: Your code goes here
				for(Text u:values) {
					context.write(u, key);
				}

				//System.out.println("over keeey");


		}
	}

	/**
	 * This method performs value-based sorting on the given input by configuring
	 * the job as appropriate and using Hadoop.
	 * 
	 * @param job
	 *          Job created for this function
	 * @param input`	
	 *          String representing location of input directory
	 * @param output
	 *          String representing location of output directory
	 * @return True if successful, false otherwise
	 * @throws Exception
	 */
	public static boolean sort(Job job, String input, String output, int cutoff)
			throws Exception {

		CUTOFF = cutoff;

		job.setJarByClass(TweetSortMR.class);

		// TODO: Set up map-reduce...

		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		job.setMapperClass(TweetSortMR.SwapMapper.class);
		job.setReducerClass(TweetSortMR.SwapReducer.class);
		job.setSortComparatorClass(IntComparator.class);


		// End

		FileInputFormat.addInputPath(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));

		return job.waitForCompletion(true);
	}

}
