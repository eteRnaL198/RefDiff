package investigation;

import java.util.Map;

public final class RepoConfig {

    public static final String[] JAVA = {
        // "https://github.com/HannahMitt/HomeMirror.git",
        // "https://github.com/nostra13/Android-Universal-Image-Loader.git",
        // "https://github.com/elastic/elasticsearch.git",
        // "https://github.com/ReactiveX/RxJava.git",
        // "https://github.com/spring-projects/spring-framework.git",
    };

    public static final String[] C = {
        // "https://github.com/b4winckler/macvim.git",
        // "https://github.com/liuliu/ccv.git",
        // "https://github.com/git/git.git",
        // "https://github.com/torvalds/linux.git",
        // "https://github.com/vmg/redcarpet.git",
    };

    public static final String[] JAVASCRIPT = {
        "https://github.com/adobe/brackets.git",
        "https://github.com/moment/moment.git",
        "https://github.com/gulpjs/gulp.git",
        "https://github.com/nodejs/node.git",
        "https://github.com/angular/angular.js.git",
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