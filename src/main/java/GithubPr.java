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
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

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

    private static List<GitHubPullRequest> gitHubPullRequests;
    private static final CloseableHttpClient httpclient = HttpClients.createDefault();
    private static final String GITHUB_API = "https://api.github.com/repos/";
    private static final String GITHUB_URL = "https://github.com/";


    public static void main(String[] args) throws IOException {
        List<GitHubPullRequest> gitHubPullRequests = getFirstPageOfPullRequestsBySDK("gradle/gradle");
    }

    // 给定一个仓库名，例如"golang/go"，或者"gradle/gradle" 返回Pull request信息
    public static List<GitHubPullRequest> getFirstPageOfPullRequests(String repo) throws IOException {
        gitHubPullRequests = new ArrayList<>();

        HttpGet httpGet = new HttpGet(GITHUB_API + repo + "/pulls");
        httpGet.setHeader("Accept", "application/vnd.github.v3+json");
        CloseableHttpResponse response = httpclient.execute(httpGet);

        try {
            HttpEntity responseEntity = response.getEntity();
            InputStream inputStream = responseEntity.getContent();
            String result = IOUtils.toString(inputStream, "UTF-8");

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(result);

            for (int i = 0; i < node.size(); i++) {
                int number = node.get(i).get("number").asInt();
                String title = node.get(i).get("title").asText();
                String author = node.get(i).get("user").get("login").asText();

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
        gitHubPullRequests = new ArrayList<>();

        HttpGet httpGet = new HttpGet(GITHUB_URL + repo + "/pulls");
        CloseableHttpResponse response = httpclient.execute(httpGet);

        try {
            HttpEntity responseEntity = response.getEntity();
            InputStream inputStream = responseEntity.getContent();
            String result = IOUtils.toString(inputStream, "UTF-8");

            Document doc = Jsoup.parse(result);
            ArrayList<Element> elements = doc.select(".js-issue-row");

            for (Element e : elements) {
                String title = e.child(0).child(1).child(0).text();
                String author = e.child(0).child(1).child(3).child(0).child(1).text();
                String[] str = e.child(0).child(1).child(3).child(0).text().split(" ");
                int number = Integer.parseInt(str[0].substring(1));
                gitHubPullRequests.add(new GitHubPullRequest(number, title, author));
            }
            EntityUtils.consume(responseEntity);
        } finally {
            response.close();
        }
        return gitHubPullRequests;
    }


    public static List<GitHubPullRequest> getFirstPageOfPullRequestsBySDK(String repo) throws IOException {
        gitHubPullRequests = new ArrayList<>();
        // 使用匿名链接，访问到 GitHub
        GitHub github = GitHub.connectAnonymously();
        // 获取对应的仓库信息
        GHRepository repository = github.getRepository(repo);
        // 返会所有状态为 OPEN 的 PR 信息
        List<GHPullRequest> pullRequests = repository.getPullRequests(GHIssueState.OPEN);

        for (GHPullRequest gh : pullRequests) {
            gitHubPullRequests.add(new GitHubPullRequest(gh.getNumber(), gh.getTitle(), gh.getUser().getLogin()));
        }

        return gitHubPullRequests;
    }


}
