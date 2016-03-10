package MTVS::Export::PDR;

=head1 NAME

MTVS::Export::PDR.pm - A Perl module used for creating PDR files.

=head1 SYNOPSIS

use MTVS::Export::PDR;
my $pdr = new MTVS::Export::PDR($PDRfile, $OrigSystem, $FileCount, $ExpirationTime);

=head1 DESCRIPTION

PDR.pm is a Perl module for creating a PDR file.  A PDR, or Product Delivery
Record, is a notice sent to users (mostly DAACs) after an Export order is
filled. It follows specific interface specification.

=head1 INPUT PARAMETERS

$PDRfile - Filename of the PDR to be created.
$OrigSystem - Origination system, one of the parameters that must be specified
              in a PDR.
$FileCount - Total number of files that the order has sent out, a PDR parameter.
$ExpirationTime - The time when the data exported expires (data can be deleted
                  if expired), a PDR parameter.

=head1 OUTPUT PARAMETERS

Standard outputs.

=head1 RETURN

Module handle.

=head1 Design Notes



=head1 AUTHORS AND MAINTAINERS

Jianfu Pan
Gary Fu

=head1 ACKNOWLEDGEMENTS

This software is developed by the MODAPS Team for the National Aeronautics and
Space Administration, Goddard Space Flight Center, under contract NAS5-32373.

=cut

# ============================================================================
# ============================================================================
# Package PDR
# ============================================================================
# ============================================================================
#
# Data:
#    PDRfname
#    State  (READY, IN-FILE-GROUP, ...)
#    ORIGINATING_SYSTEM
#    EXPIRATION_TIME
#    indexFG  = FileGroups index to push FileSpecs on = -1, 0, 1, ...
#               (incremented by setFileGroup())
#    FileGroups[]{DATA_TYPE =>, DATA_VERSION =>, NODE_NAME => }
#                {FileSpecs}[]{DIRECTORY_ID =>,
#                              FILE_ID =>,
#                              FILE_TYPE =>,
#                              FILE_SIZE =>,
#                              FILE_CKSUM_TYPE =>,
#                              FILE_CKSUM_VALUE => }
#    NumFileSpec = count of files => TOTAL_FILE_COUNT
#
# Methods:
#    new(PDRfname, os, file_count, expiration_time)
#    AddFileGroup(data_type, data_ver, node)
#    AddFileSpec(ref_filespec_hash)
#    ClosePDR()
#

# ==============================================
# new(PDRfname, os, file_count, expiration_time)
# ==============================================

sub new
{
   my $class = shift;
   my ($PDRfname, $os, $expiration_time) = @_;

   my $self = { PDRfname           => $PDRfname,
		ORIGINATING_SYSTEM => $os,
		EXPIRATION_TIME    => $expiration_time,
		indexFG            => -1,
		FileGroups         => [],
		NumFileSpec        => 0,
		};

   bless $self, $class;

   return $self;
}


sub AddFileGroup
{
   my $self = shift;
   my ($data_type, $data_ver, $node) = @_;

   $self->{indexFG}++;
   $self->{FileGroups}[$self->{indexFG}] = {DATA_TYPE    => $data_type, 
					    DATA_VERSION => $data_ver,
					    NODE_NAME    => $node,
					    FileSpecs    => [],
					    };
}


sub AddFileSpec
{
   my $self = shift;
   my ($f) = @_;
   die "E CODE Error: No file group is open." if ($self->{indexFG} == -1);

   push @{$self->{FileGroups}[$self->{indexFG}]{FileSpecs}},
   {DIRECTORY_ID => $$f{DIRECTORY_ID},
    FILE_ID      => $$f{FILE_ID},
    FILE_TYPE    => $$f{FILE_TYPE},
    FILE_SIZE    => $$f{FILE_SIZE},
    FILE_CKSUM_TYPE  => $$f{FILE_CKSUM_TYPE},
    FILE_CKSUM_VALUE => $$f{FILE_CKSUM_VALUE},
   };
   $self->{NumFileSpec}++;
}


# Write the PDR object into specified filename
sub ClosePDR
{
   my $self = shift;

   die "E ERROR: PDR with no files" unless ($self->{NumFileSpec});

   open(PDRH, ">$self->{PDRfname}") || die "Cannot open PDRH: $PDRfname\n";

   print PDRH "ORIGINATING_SYSTEM = $self->{ORIGINATING_SYSTEM};\n".
              "TOTAL_FILE_COUNT = $self->{NumFileSpec};\n".
              "EXPIRATION_TIME = $self->{EXPIRATION_TIME};\n";

   foreach my $fg (@{$self->{FileGroups}}) {
       print PDRH "OBJECT=FILE_GROUP;\n".
	          " DATA_TYPE = $fg->{DATA_TYPE};\n".
		  " DATA_VERSION = $fg->{DATA_VERSION};\n".
                  " NODE_NAME = $fg->{NODE_NAME};\n";

       foreach my $fs (@{$fg->{FileSpecs}}) {
	   print PDRH " OBJECT = FILE_SPEC;\n".
	              "  DIRECTORY_ID = $fs->{DIRECTORY_ID};\n".
                      "  FILE_ID = $fs->{FILE_ID};\n".
                      "  FILE_TYPE = $fs->{FILE_TYPE};\n".
                      "  FILE_SIZE = $fs->{FILE_SIZE};\n";

           print PDRH "  FILE_CKSUM_TYPE = $fs->{FILE_CKSUM_TYPE};\n".
                      "  FILE_CKSUM_VALUE = $fs->{FILE_CKSUM_VALUE};\n" 
               if (defined $fs->{FILE_CKSUM_TYPE} and $fs->{FILE_CKSUM_TYPE} ne ''); 
           
           print PDRH " END_OBJECT = FILE_SPEC;\n";
       }

       print PDRH "END_OBJECT = FILE_GROUP;\n";
   }

   close(PDRH);
}

1;

