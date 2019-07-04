package cmsc433.p5;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * Map reduce which takes in a CSV file with tweets as input and output
 * key/value pairs.</br>
 * </br>
 * The key for the map reduce depends on the specified {@link TrendingParameter}
 * , <code>trendingOn</code> passed to
 * {@link #score(Job, String, String, TrendingParameter)}).
 */
public class TweetPopularityMR {

	// For your convenience...
	public static final int          TWEET_SCORE   = 1;
	public static final int          RETWEET_SCORE = 2;
	public static final int          MENTION_SCORE = 1;
	public static final int			 PAIR_SCORE = 1;

	// Is either USER, TWEET, HASHTAG, or HASHTAG_PAIR. Set for you before call to map()
	private static TrendingParameter trendingOn;

	public static class TweetMapper
	extends Mapper<Object,Text,Text,IntWritable> {

		@Override
		public void map(/* Fill in type */Object key, Text/* Fill in type */ value, Context context)
				throws IOException, InterruptedException {
			// Converts the CSV line into a tweet object
			Tweet tweet = Tweet.createTweet(value.toString());
			//System.out.println("this is value tostring:"+value.toString());
			// TODO: Your code goes here
			if(trendingOn==TrendingParameter.TWEET) {
				Text id=new Text(tweet.getId().toString());
				IntWritable TS=new IntWritable(TWEET_SCORE);
				context.write(id, TS);
				
				if(tweet.getRetweetedTweet()!=null) {
					Text rt=new Text(tweet.getRetweetedTweet().toString());
					IntWritable RS=new IntWritable(RETWEET_SCORE);
					context.write(rt,RS);
				}
				
			}else if(trendingOn==TrendingParameter.HASHTAG) {
				if(!tweet.getHashtags().isEmpty()) {
					for(String h:tweet.getHashtags()){
						Text hash=new Text(h);
						IntWritable RScore=new IntWritable(PAIR_SCORE);
						context.write(hash,RScore);
					}
				}
				
			}else if(trendingOn==TrendingParameter.HASHTAG_PAIR) {
				LinkedList<String> hList=new LinkedList<> (tweet.getHashtags());
				while(hList.size()!=0) {
					String temp=hList.pop();
					for(String temp1:hList) {
						IntWritable PScore=new IntWritable(PAIR_SCORE);
						if(temp.compareTo(temp1)>0) {
							Text hash=new Text("("+temp1+","+temp+")");
							context.write(hash,PScore);
						}else {
							Text hash=new Text("("+temp+","+temp1+")");
							context.write(hash,PScore);
						}
					}
				}
				
				
			}else if(trendingOn==TrendingParameter.USER) {
				Text username=new Text(tweet.getUserScreenName());
				IntWritable TS=new IntWritable(TWEET_SCORE);
				context.write(username, TS);
				
				if(!tweet.getMentionedUsers().isEmpty()) {
					for(String MU:tweet.getMentionedUsers()) {
						Text MUser=new Text(MU);
						IntWritable MScore=new IntWritable(MENTION_SCORE);
						context.write(MUser,MScore);
					}
				}
				
				if(tweet.wasRetweetOfUser()) {
					Text Reuser=new Text(tweet.getRetweetedUser());
					IntWritable RScore=new IntWritable(RETWEET_SCORE);
					context.write(Reuser,RScore);
				}
				
			}else {
				return;
			}
			



		}
	}

	public static class PopularityReducer
	extends Reducer<Text, IntWritable,Text,IntWritable> {

		@Override
		public void reduce(/* Fill in type */Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {

			// TODO: Your code goes here
			int total=0;
			for(IntWritable temp:values) {
				total=total+temp.get();
			}
				
			IntWritable temp=new IntWritable(total);
			context.write(key,temp);



		}
	}

	/**
	 * Method which performs a map reduce on a specified input CSV file and
	 * outputs the scored tweets, users, or hashtags.</br>
	 * </br>
	 * 
	 * @param job
	 * @param input
	 *          The CSV file containing tweets
	 * @param output
	 *          The output file with the scores
	 * @param trendingOn
	 *          The parameter on which to score
	 * @return true if the map reduce was successful, false otherwise.
	 * @throws Exception
	 */
	public static boolean score(Job job, String input, String output,
			TrendingParameter trendingOn) throws Exception {

		TweetPopularityMR.trendingOn = trendingOn;

		job.setJarByClass(TweetPopularityMR.class);

		// TODO: Set up map-reduce...

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setMapperClass(TweetPopularityMR.TweetMapper.class);
		job.setReducerClass(TweetPopularityMR.PopularityReducer.class);




		// End

		FileInputFormat.addInputPath(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));

		return job.waitForCompletion(true);
	}
}
