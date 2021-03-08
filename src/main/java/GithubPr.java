import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zzf
 * @date 2021/3/5/005 12:32
 */
public class GithubPr {

    static class GitHubPullRequest {
        // Pull request的编号
        int number;
        // Pull request的标题
        String title;
        // Pull request的作者的 GitHub 用户名
        String author;

        GitHubPullRequest(int number, String title, String author) {
            this.number = number;
            this.title = title;
            this.author = author;
        }
    }

    public static void main(String[] args) throws IOException {
        List<GitHubPullRequest> gitHubPullRequests = getFirstPageOfPullRequestsByHtml("gradle/gradle");
    }

    // 给定一个仓库名，例如"golang/go"，或者"gradle/gradle" 返回Pull request信息
    public static List<GitHubPullRequest> getFirstPageOfPullRequests(String repo) throws IOException {
        List<GitHubPullRequest> gitHubPullRequests = new ArrayList<>();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://api.github.com/repos/" + repo + "/pulls");
        httpGet.setHeader("Accept", "application/vnd.github.v3+json");
        CloseableHttpResponse response = httpclient.execute(httpGet);

        try {
            HttpEntity responseEntity = response.getEntity();
            InputStream inputStream = responseEntity.getContent();
            String result = IOUtils.toString(inputStream, "UTF-8");

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(result);

            int number;
            String title;
            String author;
            for (int i = 0; i < node.size(); i++) {
                number = node.get(i).get("number").asInt();
                title = node.get(i).get("title").asText();
                author = node.get(i).get("user").get("login").asText();

                gitHubPullRequests.add(new GitHubPullRequest(number, title, author));
            }
            EntityUtils.consume(responseEntity);
        } finally {
            response.close();
        }
        return gitHubPullRequests;
    }

    // 给定一个仓库名，例如"golang/go"，或者"gradle/gradle" 返回Pull request信息
    public static List<GitHubPullRequest> getFirstPageOfPullRequestsByHtml(String repo) throws IOException {
        List<GitHubPullRequest> gitHubPullRequests = new ArrayList<>();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://github.com/" + repo + "/pulls");
        CloseableHttpResponse response = httpclient.execute(httpGet);

        try {
            HttpEntity responseEntity = response.getEntity();
            InputStream inputStream = responseEntity.getContent();
            String result = IOUtils.toString(inputStream, "UTF-8");

            Document doc = Jsoup.parse(result);
            ArrayList<Element> elements = doc.select(".js-issue-row");

            int number;
            String title;
            String author;
            for (Element e : elements) {
                title = e.child(0).child(1).child(0).text();
                author = e.child(0).child(1).child(3).child(0).child(1).text();
                String[] str = e.child(0).child(1).child(3).child(0).text().split(" ");
                number = Integer.parseInt(str[0].substring(1));
                gitHubPullRequests.add(new GitHubPullRequest(number, title, author));
            }
            EntityUtils.consume(responseEntity);
        } finally {
            response.close();
        }
        return gitHubPullRequests;
    }


}
