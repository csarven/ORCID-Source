var gulp = require('gulp'),
ts = require('gulp-typescript'),
watch = require('gulp-watch'),
livereload = require('gulp-livereload'),
plumber = require('gulp-plumber');

gulp.task('typescript', function () {
    return gulp.src('app/src/**/*.ts')
    .pipe(plumber())
    .pipe(ts({
        noImplicitAny: true,
        out: 'angular2Orcid.js'
    }))
    .pipe(gulp.dest('app/dist'))    
    .pipe(livereload());    
});

gulp.task('watch', function() {
    livereload.listen();
    gulp.watch('app/src/**/*.ts', ['typescript']);
});

gulp.task('default', ['watch']);