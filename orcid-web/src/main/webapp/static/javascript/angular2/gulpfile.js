var gulp = require('gulp');
var ts = require('gulp-typescript');
var watch = require('gulp-watch');

gulp.task('default', function () {    
    
    return gulp.src('app/src/**/*.ts')
    .pipe(ts({
        noImplicitAny: true,
        out: 'angular2Orcid.js'
    }))
    .pipe(gulp.dest('app/dist'));

    
});