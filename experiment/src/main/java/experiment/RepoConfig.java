package experiment;

import java.util.Map;

public final class RepoConfig {

    public static final String[] JAVA = {
        // "https://github.com/iluwatar/java-design-patterns.git",
        // "https://github.com/spring-projects/spring-boot.git",
        // "https://github.com/Stirling-Tools/Stirling-PDF.git",
    };

    public static final String[] C = {
        // "https://github.com/torvalds/linux.git",
        // "https://github.com/Genymobile/scrcpy.git",
        // "https://github.com/netdata/netdata.git",
        // "https://github.com/redis/redis.git",
        // "https://github.com/obsproject/obs-studio.git",
        // "https://github.com/curl/curl.git",
        // "https://github.com/tmux/tmux.git",
    };

    public static final String[] JAVASCRIPT = {
        // "https://github.com/facebook/react.git",
        // "https://github.com/airbnb/javascript.git",
        // "https://github.com/vercel/next.js.git",
        // "https://github.com/nodejs/node.git",
    };

    public static final String[] RUBY = {
        // "https://github.com/rails/rails.git",
        // "https://github.com/maybe-finance/maybe.git",
    };

    public static final String[] GO = {
        // "https://github.com/ollama/ollama.git",
        // "https://github.com/golang/go.git",
    };

    public static final String[] PHP = {
        // "https://github.com/laravel/framework.git",
        // "https://github.com/nextcloud/server.git",
    };

    public static final String[] PYTHON = {
        // "https://github.com/Significant-Gravitas/AutoGPT.git",
        // "https://github.com/huggingface/transformers.git",
        // "https://github.com/ytdl-org/youtube-dl.git"
    };

    public static final Map<String, String[]> REPOS_BY_LANGUAGE = Map.of(
        "Java", JAVA,
        "C", C,
        "JavaScript", JAVASCRIPT,
        "Ruby", RUBY,
        "Go", GO,
        "PHP", PHP,
        "Python", PYTHON
    );

    private RepoConfig() {}
}