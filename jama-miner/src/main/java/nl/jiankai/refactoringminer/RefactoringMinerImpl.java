package nl.jiankai.refactoringminer;

import gr.uom.java.xmi.diff.*;
import nl.jiankai.api.*;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class RefactoringMinerImpl implements RefactoringMiner {
    private final MethodQuery methodQuery;

    public RefactoringMinerImpl(MethodQuery methodQuery) {
        this.methodQuery = methodQuery;
    }

    @Override
    public Collection<Refactoring> detectRefactoringBetweenCommit(GitRepository gitRepository, String startCommitId, String endCommitId, Set<RefactoringType> refactoringTypes) {
        List<Refactoring> detectedRefactorings = new ArrayList<>();
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();
        try (Repository repository = gitService.openRepository(gitRepository.getLocalPath().getAbsolutePath())) {
            gitHistoryRefactoringMiner.detectBetweenCommits(repository, startCommitId, endCommitId, new RefactoringHandler() {
                int sequence = 1;

                @Override
                public void handle(String commitId, List<org.refactoringminer.api.Refactoring> refactorings) {
                    List<Refactoring> foundRefactoring = refactorings
                            .stream()
                            .filter(r -> {
                                RefactoringType refactoringType = convertRefactoringType(r.getRefactoringType());
                                return refactoringType != RefactoringType.UNKNOWN && (refactoringTypes.isEmpty() || refactoringTypes.contains(refactoringType));
                            })
                            .map(r -> new Refactoring(commitId, sequence, getBeforeElement(r), getAfterElement(r), convertRefactoringType(r.getRefactoringType())))
                            .toList();
                    if (!foundRefactoring.isEmpty()) {
                        detectedRefactorings.addAll(foundRefactoring);
                        sequence++;
                    }
                }
            });

            return detectedRefactorings;
        } catch (Exception e) {
            throw new IllegalArgumentException("Something went wrong with the repository '%s'".formatted(gitRepository.getId()), e);
        }
    }

    private Refactoring.CodeElement getBeforeElement(org.refactoringminer.api.Refactoring refactoring) {
        String filePath = getBeforeFilePath(refactoring);
        Position position = getBeforePosition(refactoring);
        return new Refactoring.CodeElement(getBeforeElementName(refactoring), getBeforePackagePath(refactoring), null, position, filePath);
    }

    private Refactoring.CodeElement getAfterElement(org.refactoringminer.api.Refactoring refactoring) {
        String filePath = getAfterFilePath(refactoring);
        Position position = getAfterPosition(refactoring);
        return new Refactoring.CodeElement(getAfterElementName(refactoring), getAfterPackagePath(refactoring), null, position, filePath);
    }

    private String getFullyQualifiedClassName(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getClassName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getClassName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getClassName();
        }

        return "";
    }

    private String getBeforeFilePath(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getLocationInfo().getFilePath();
        }

        return "";
    }

    private Position getBeforePosition(org.refactoringminer.api.Refactoring refactoring) {
        CodeRange codeRange = null;
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            codeRange = crtr.getOperationBefore().codeRange();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            codeRange = apr.getOperationBefore().codeRange();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            codeRange = rpr.getOperationBefore().codeRange();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            codeRange = cvtr.getOperationBefore().codeRange();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            codeRange = ror.getSourceOperationCodeRangeBeforeRename();
        }

        if (codeRange == null) {
            return null;
        } else {
            return new Position(codeRange.getStartColumn(), codeRange.getEndColumn(), codeRange.getStartLine(), codeRange.getEndLine());
        }
    }

    private String getBeforeElementName(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationBefore().getName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getName();
        }

        return "";
    }

    private String getBeforePackagePath(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getClassName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getClassName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getClassName();
        }

        return "";
    }

    private String getAfterFilePath(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getRenamedOperation().getLocationInfo().getFilePath();
        }

        return "";
    }

    private Position getAfterPosition(org.refactoringminer.api.Refactoring refactoring) {
        CodeRange codeRange = null;
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            codeRange = crtr.getOperationAfter().codeRange();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            codeRange = apr.getOperationAfter().codeRange();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            codeRange = rpr.getOperationAfter().codeRange();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            codeRange = cvtr.getOperationAfter().codeRange();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            codeRange = ror.getTargetOperationCodeRangeAfterRename();
        }

        if (codeRange == null) {
            return null;
        } else {
            return new Position(codeRange.getStartColumn(), codeRange.getEndColumn(), codeRange.getStartLine(), codeRange.getEndLine());
        }
    }

    private String getAfterElementName(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationAfter().getName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationAfter().getName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationAfter().getName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationAfter().getName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getRenamedOperation().getName();
        }

        return "";
    }

    private String getAfterPackagePath(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationAfter().getClassName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationAfter().getClassName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getRenamedOperation().getClassName();
        }

        return "";
    }


    private RefactoringType convertRefactoringType(org.refactoringminer.api.RefactoringType type) {
        return switch (type) {
            case RENAME_METHOD -> RefactoringType.METHOD_NAME;
            case CHANGE_RETURN_TYPE -> RefactoringType.CHANGE_RETURN_TYPE;
            case CHANGE_PARAMETER_TYPE -> RefactoringType.CHANGE_PARAMETER_TYPE;
            case REMOVE_PARAMETER -> RefactoringType.REMOVE_PARAMETER;
            case ADD_PARAMETER -> RefactoringType.ADD_PARAMETER;
            case ENCAPSULATE_ATTRIBUTE -> RefactoringType.ENCAPSULATE_ATTRIBUTE;
            case MOVE_CLASS -> RefactoringType.MOVE_CLASS;
            case RENAME_CLASS -> RefactoringType.CLASS_NAME;
            case RENAME_PACKAGE -> RefactoringType.PACKAGE_NAME;
            default -> RefactoringType.UNKNOWN;
        };
    }
}
