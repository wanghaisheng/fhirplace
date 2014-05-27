module.exports = function (grunt) {
  grunt.loadNpmTasks('grunt-contrib-coffee');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-angular-templates');
  //grunt.loadNpmTasks('grunt-shell');
  grunt.loadNpmTasks('grunt-contrib-connect');

  grunt.initConfig({
    clean: {
      options: { force: true },
      main: ['../resources/public/assets/**/*']
    },
    coffee: {
      app: {
        options: { join: true },
        files: { '../resources/public/assets/js/main.js': 'coffee/**/*.coffee' }
      }
    },
   ngtemplates: {
      app: {
        src: 'views/**/*.html',
        dest: '../resources/public/assets/js/views.js',
        options: {
          module: 'fhirplaceGui',
          prefix: '/'
        }
      }
    },
    less: {
      dist: {
        files: [{
          expand: true,
          cwd: 'less',
          src: ['*.less', '!.*#.less'],
          dest: '../resources/public/assets/css/',
          ext: '.css'
        }]
      }
    },
    concat: {
      lib_js: {
        src: [
          "lib/jquery/dist/jquery.min.js",
          "lib/angular/angular.js",
          "lib/angular-formstamp/build/formstamp.js",
          "lib/angular-route/angular-route.js",
          "lib/angular-animate/angular-animate.js",
          "lib/angular-cookies/angular-cookies.js",
          "lib/angular-sanitize/angular-sanitize.js",
          "lib/codemirror/lib/codemirror.js",
          "lib/codemirror/mode/sql/sql.js",
          "lib/codemirror/mode/javascript/javascript.js",
          "lib/angular-ui-codemirror/ui-codemirror.js"
        ],
        dest: '../resources/public/assets/js/lib.js'
      },
      app_js: {
        src: [ '../resources/public/assets/js/main.js',
        '../resources/public/assets/js/views.js' ],
        dest: '../resources/public/assets/js/app.js'
      },
      lib_css: {
        src: ['lib/components-font-awesome/css/font-awesome.min.css',
        'lib/codemirror/lib/codemirror.css',
        'lib/bootstrap/dist/css/bootstrap.min.css',
        "lib/angular-formstamp/build/formstamp.css",
        ],
        dest: '../resources/public/assets/css/lib.css'
      }
    },
    copy: {
      bs_fonts: {
        cwd: 'lib/bootstrap/dist/fonts/',
        expand: true,
        src: '*',
        dest: '../resources/public/assets/fonts/'
      },
      fa_fonts: {
        cwd: 'lib/components-font-awesome/fonts/',
        expand: true,
        src: '*',
        dest: '../resources/public/assets/fonts/'
      },
      hs_fonts: {
        cwd: 'fonts/',
        expand: true,
        src: '*',
        dest: '../resources/public/assets/fonts/'
     },
     index: {
       src: 'index.html',
       dest: '../resources/public/index.html'
     }
    },
    watch: {
      main: {
        files: ['views/**/*', 'index.html','coffee/**/*.coffee', 'less/**/*.less'],
        tasks: ['build'],
        options: {
          events: ['changed', 'added'],
          nospawn: true
        }
      }
    }
  });

  grunt.registerTask('build', ['clean', 'coffee', 'less', 'ngtemplates', 'concat', 'copy']);
};
