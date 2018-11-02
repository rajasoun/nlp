import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UnitCellMultiplication {
    public static class TransitionMapper extends Mapper<Object, Text, Text, Text> {
        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] fromTo = value.toString().trim().split("\t");

            // if there is no tos, then this website is a dead end
            if (fromTo.length == 1) {
                return;
            }

            String outputKey = fromTo[0];
            String[] tos = fromTo[1].split(",");

            double probability = 1.0 / tos.length;
            for (String to : tos) {
                String outputValue = to + "=" + String.valueOf(probability);
                context.write(new Text(outputKey), new Text(outputValue));
            }
        }
    }

    public static class PRMapper extends Mapper<Object, Text, Text, Text> {
        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] idPr = value.toString().trim().split("\t");
            String outputKey = idPr[0];
            String outputValue = idPr[1];
            context.write(new Text(outputKey), new Text(outputValue));
        }
    }

    public static class MultiplicationReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            List<String> tos = new ArrayList<>();
            double prePr = 0;
            for (Text value : values) {
                String str = value.toString();
                if (str.contains("=")) {
                    tos.add(str);
                }
                else {
                    prePr = Double.valueOf(str);
                }
            }

            for (String to : tos) {
                String[] toProb = to.split("=");
                String outputKey = toProb[0];
                double prob = Double.valueOf(toProb[1]);
                String outputValue = String.valueOf(prob * prePr);
                context.write(new Text(outputKey), new Text(outputValue));
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration configuration = new Configuration();
        Job job = Job.getInstance(configuration);
        job.setJarByClass(UnitCellMultiplication.class);

        job.setReducerClass(MultiplicationReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        //import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
        //import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
        //import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, TransitionMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, PRMapper.class);

        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        job.waitForCompletion(true);
    }
}
