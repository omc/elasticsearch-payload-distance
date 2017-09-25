require 'json'

module ES
  class PluginBuilder

    attr_reader :es_version

    def initialize es_version, dir="."
      @es_version = es_version == "resolve" ? props["esVersion"] : es_version
      @dir = dir
    end

    def build! build_dir=".local/builds/"
      check_system!
      system "esVersion=#{es_version} #{resolve_gradle} clean assemble"
      system "cp build/distributions/#{zip_name}* #{build_dir}"
    end

    def run!
      cmd = [
        "#{resolve_gradle} clean assemble",
        "cd #{ENV['ES_INSTALL_ROOT']}",
        "bin/elasticsearch-plugin remove #{name} || true",
        "rm -rf config/#{name}",
        "bin/elasticsearch-plugin install --silent file://#{File.dirname(__FILE__)}/build/distributions/#{zip_name}",
        "BONSAI_TEST_MODE=true bin/elasticsearch"
      ].join(" && ")
      system cmd
    end

    def check_system!
      puts "--------------------"
      puts "Checking Environment"
      puts "--------------------"

      # Get the gradle path
      bin = resolve_gradle
      if bin.nil?
        error "bonsai-plugin requires gradle (2.13 and >= 3.53): https://gradle.org/install"
      else
        success "Found gradle #{bin}"
      end

      # Setup local ES dir for integration tests
      unless File.exist?(".local/builds")
        info "Creating local cache dir"
        system "mkdir -p .local/builds"

        if $? == 0
          success "Created .local dir for repo cache"
        else
          error "Unable to make .local dir!"
        end
      end

      unless File.exists?(".local/elasticsearch")
        info "Fetching elasticsearch repo via git"
        system "cd .local && git clone https://github.com/elastic/elasticsearch.git"
        if $? == 0
          success "Elasticsearch repo cloned"
        else
          error "Failed to clone repo!"
        end
      end

      info "Checking out elasticsearch tag v#{es_version}"
      system "cd .local/elasticsearch && git checkout v#{es_version}"
      if $? == 0
        success "Head @ v#{es_version} checked out"
      else
        info "Failed to checkout v#{es_version}, trying something..."
        system "cd .local/elasticsearch && git fetch origin && git checkout v#{es_version}"
        if $? != 0
          error "Dang... I tried to fetch and checkout, but something isn't working! is the version real!?"
        end
        success "Yay, fixed it!"
      end

      success "All Good!"
    end

    private

    def error message, code=1
      puts "[\e[31mFAIL\e[0m]: #{message}"
      exit code
    end

    def success message
      puts "[\e[32mDONE\e[0m]: #{message}"
    end

    def info message
      puts "[\e[36mINFO\e[0m]: #{message}"
    end

    def props
      Hash[*File.read("gradle.properties").split("\n").map { |l| l.split("=") }.flatten]
    end

    def name
      props["pluginName"]
    end

    def version
      props["pluginVersion"]
    end

    def zip_name
      "#{name}-#{version}_es#{es_version}.zip"
    end

    def gradle_bin version
      symlink = ".local/gradle-#{version}"
      unless File.exists?(symlink)
        print "Enter path for gradle version #{version}\n> "
        path = $stdin.gets.strip
        system "mkdir -p .local && ln -sf #{path} #{symlink}"
      end
      symlink
    end

    def resolve_gradle
      [
        [lambda { |x| x <  5.4 }, lambda { gradle_bin("2.13") }],
        [lambda { |x| x >= 5.4 }, lambda { gradle_bin("3.5") }]
      ].select { |check, _|
        check.call(es_version.to_f)
      }.map { |_, supplier|
        supplier.call
      }.first
    end

  end
end


##############
##########################
#######################################

desc "Compile"
task :build do
  ES::PluginBuilder.new(ENV.fetch("esVersion", "resolve")).build!

  puts "Copying to .cache"
  system "mkdir -p .cache"
  system "cp build/distributions/*.zip* .cache/"
end

desc "Build plugins for a plurality of elasticsearch releases"
task :mbuild do
  versions = ENV["esVersions"]
  if versions.nil?
    raise "mbuild requires esVersions env - TODO: YAML"
  end

  versions.split(",").each do |v|
    ES::PluginBuilder.new(v).build!
  end
end

desc "Compile, install, run es with plugin"
task :dev_loop do
  ES::PluginBuilder.new(ENV.fetch("esVersion", "resolve")).run!
end

desc "Index some data and run a test query"
task :test_search do

  def curl path, method="GET", data={}
    puts "#{method} #{path}"
    system "curl -X#{method} 'http://localhost:9200#{path}' -d '#{JSON.pretty_generate(data)}'"
    puts
  end

  curl "/items", "DELETE"
  curl "/items", "PUT", {
    settings: {
      number_of_shards: 1,
      number_of_replicas: 0,
      analysis: {
        analyzer: {
          payloads: {
            type: "custom",
            tokenizer: "whitespace",
            filter: [
              "lowercase",
              "delimited_payload_filter"
            ]
          }
        }
      }
    },
    mappings: {
      human: {
        properties: {
          simterms: {
            type:     "text",
            analyzer: "payloads",
            term_vector: "with_positions_offsets_payloads"
          },
          colors: {
            type:     "text",
            analyzer: "payloads",
            term_vector: "with_positions_offsets_payloads"
          }
        }
      }
    }
  }

  curl "/items/outfit/1", "PUT", { name: "flowing dress", simterms: "a|100.0 b|100.0 c|100.0", colors: "blue|100" }
  curl "/items/outfit/2", "PUT", { name: "blue dress", simterms: "a|200.0 b|100.0 c|50.0", colors: "green|100" }
  curl "/items/outfit/3", "PUT", { name: "moo dress bing", simterms: "a|300.0 b|100.0 c|50.0", colors: "red|100" }
  curl "/items/_refresh", "POST"

  query = {
    query: {
      function_score: {
        query: { match: { name: "dress" } },
        script_score: {
          script: {
            lang: "native",
            inline: "payload_distance_score",
            params: {
              fields: [{
                field: "simterms",
                term_values: {
                  "a": 100.0,
                  "b": 100.0,
                  "d": 100.0
                },
                term_missing_factor: 0.2,
                term_match_boost: 0.0005
              },{
                field: "colors",
                term_values: {
                  "green": 100.0
                },
                term_missing_factor: 0.2,
                term_match_boost: 0.0025
              }]

            }
          }
        }
      }
    }
  }

  curl "/items/_search?pretty", "GET", query

end
