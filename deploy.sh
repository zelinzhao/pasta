rm -rf .repo
mkdir .repo
cp -r * .gitignore .repo/
cd .repo && git init . && git add . && git commit -m 'ghpage' && \
  git remote add github git@github.com:zelinzhao/pasta.git && \
  git push github HEAD:gh-pages --force

