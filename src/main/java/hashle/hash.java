package hashle;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.security.NoSuchAlgorithmException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

public class hash extends Configured implements Tool {

	/**
	 * Mapper class
	 * 
	 */
	static String[] hashler;

	static class oku extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, Text> {

		private Text word = new Text();
		private final static IntWritable ONE = new IntWritable(1);

		String myStringOption;

		@Override
		public void configure(JobConf job) {
			super.configure(job);

			// nb: last arg is the default value if option is not set
			myStringOption = job.get("myStringOption", "notSet");
			hashler = myStringOption.split(",");
			for (int i = 0; i < hashler.length; i++) {
				hashler[i] = hashler[i].trim();
			}
			hashler[0] = hashler[0].substring(1);
			hashler[hashler.length - 1] = hashler[hashler.length - 1]
					.substring(0, hashler[hashler.length - 1].length() - 1);

		}

		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text> collector, Reporter arg3)
				throws IOException {
			BufferedReader br = new BufferedReader(new FileReader(
					"dictionary.txt"));

			String line;
			try {

				line = br.readLine();

				while (line != null) {

					line = br.readLine();
					MessageDigest md;
					try {
						md = MessageDigest.getInstance("MD5");
						md.update(line.getBytes());

						byte byteData[] = md.digest();

						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < byteData.length; i++)
							sb.append(Integer.toString(
									(byteData[i] & 0xff) + 0x100, 16)
									.substring(1));

						System.out.println("Digest(in hex format):: "
								+ sb.toString());

						collector.collect(new Text(line),
								new Text(sb.toString()));// reducera giden yer
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} finally {
				br.close();
			}

		}

	}

	/**
	 * Reducer Class
	 * 
	 */
	static class yaz extends MapReduceBase implements
			Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			// TODO Auto-generated method stub
			String deger;
			while (values.hasNext()) {
				deger = values.next().toString();
				for (int i = 0; i < hashler.length; i++) {
					if (hashler[i].equals(deger)) {
						output.collect(key, new Text(deger));
					}
				}
			}

		}

	}

	/**
	 * Entry point
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.exit(ToolRunner.run(new Configuration(), new hash(), args));
	}

	/**
	 * Run the job
	 */
	public int run(String[] args) throws Exception {

		BufferedReader br = new BufferedReader(new FileReader("sifreler.txt"));

		ArrayList<String> sifreler = new ArrayList<String>();

		try {

			String line = br.readLine();

			while (line != null) {

				sifreler.add(line);
				line = br.readLine();
			}

		} finally {
			br.close();
		}

		Configuration conf = getConf();
		JobConf job = new JobConf(conf, hash.class);

		Path pIn = new Path(args[0]);
		Path pOut = new Path(args[1]);
		FileInputFormat.setInputPaths(job, pIn);
		FileOutputFormat.setOutputPath(job, pOut);

		conf.set("my-array-list", sifreler.toString());

		job.setJobName("hash");
		job.setMapperClass(oku.class);
		job.setReducerClass(yaz.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		JobClient.runJob(job);

		return 0;
	}

}